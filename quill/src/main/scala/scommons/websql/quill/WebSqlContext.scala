package scommons.websql.quill

import io.getquill._
import io.getquill.context.sql.SqlContext
import io.getquill.idiom.Idiom
import io.getquill.monad.IOMonad
import io.getquill.util.Messages
import scommons.websql.{Database, ResultSet, Transaction}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Success, Try}

abstract class WebSqlContext[I <: Idiom, N <: NamingStrategy](val idiom: I,
                                                              val naming: N,
                                                              val db: Database)
  extends SqlContext[I, N]
    with IOMonad {

  override type PrepareRow = List[js.Any]
  override type ResultRow = WebSqlRow

  override type Result[T] = T
  override type RunQueryResult[T] = IO[Seq[T], Effect.Read]
  override type RunQuerySingleResult[T] = IO[T, Effect.Read]
  override type RunActionResult = IO[Long, Effect.Write]
  override type RunActionReturningResult[T] = IO[Long, Effect.Write]
  override type RunBatchActionResult = IO[Seq[Long], Effect.Write]
  override type RunBatchActionReturningResult[T] = IO[Seq[Long], Effect.Write]

  override def close(): Unit = ()

  def probe(statement: String): Try[_] = Success(())

  private sealed trait SqlCommand[T] {
    def sql: String
    def args: Seq[js.Any]
  }

  private case class ExecQuery[T](sql: String, args: Seq[js.Any], extractor: Extractor[T])
    extends SqlCommand[List[T]]
  
  private case class ExecAction(sql: String, args: Seq[js.Any]) extends SqlCommand[Long]
  private case class ExecActionReturning(sql: String, args: Seq[js.Any]) extends SqlCommand[Long]

  def executeQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T]): IO[List[T], Effect.Read] = {
    val (_, values) = prepare(Nil)
    Run(ExecQuery(sql, values, extractor))
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare, extractor: Extractor[T]): IO[T, Effect.Read] = {
    executeQuery(sql, prepare, extractor).map(handleSingleResult)
  }

  def executeAction(sql: String, prepare: Prepare): IO[Long, Effect.Write] = {
    val (_, values) = prepare(Nil)
    Run(ExecAction(sql, values))
  }

  def executeActionReturning(sql: String, prepare: Prepare,
                             extractor: Extractor[Int],
                             returningColumn: ReturnAction): IO[Long, Effect.Write] = {
    
    val (_, values) = prepare(Nil)
    Run(ExecActionReturning(sql, values))
  }

  def executeBatchAction(groups: List[BatchGroup]): IO[Seq[Long], Effect.Write] = {
    IO.sequence(groups.map {
      case BatchGroup(sql, prepareList) =>
        IO.sequence(prepareList.map { prepare =>
          executeAction(sql, prepare)
        })
    }).map(_.flatten)
  }

  def executeBatchActionReturning(groups: List[BatchGroupReturning],
                                  extractor: Extractor[Int]
                                 ): IO[Seq[Long], Effect.Write] = {

    IO.sequence(groups.map {
      case BatchGroupReturning(sql, column, prepareList) =>
        IO.sequence(prepareList.map { prepare =>
          executeActionReturning(sql, prepare, extractor, column)
        })
    }).map(_.flatten)
  }

  private case class Run[T, E <: Effect](cmd: SqlCommand[T]) extends IO[T, E]

  def performIO[T, E <: Effect](io: IO[T, E]): Future[T] = {

    def flatten[Y, M[X] <: IterableOnce[X]](seq: Sequence[Y, M, Effect]): IO[M[Y], Effect] = {
      seq.in.iterator.foldLeft(IO.successful(seq.cbfResultToValue.newBuilder)) { (builder, item) =>
        builder.flatMap(b => item.map(b += _))
      }.map(_.result())
    }
    
    def extractResult[R](cmd: SqlCommand[R], resultSet: ResultSet): R = {
      val result = cmd match {
        case q: ExecQuery[_] =>
          resultSet.rows.map { row =>
            val res = js.Object.keys(row.asInstanceOf[js.Object])
              .map(k => row.selectDynamic(k).asInstanceOf[js.Any])

            q.extractor(new WebSqlRow(res))
          }.toList
        case ExecAction(_, _) => resultSet.rowsAffected
        case ExecActionReturning(sql, _) => resultSet.insertId.getOrElse(
          Messages.fail(s"insertId is required, but wasn't returned: $sql")
        )
      }
      
      result.asInstanceOf[R]
    }
    
    var result: Any = null

    def loop[R](io: IO[R, _], stack: List[Try[_] => IO[_, _]])(implicit tx: Transaction): Unit = {
      io match {
        case FromTry(v) => stack match {
          case Nil => result = v.get
          case f :: tail => loop(f(v), tail)
        }
        case Run(cmd) =>
          tx.executeSql(
            sqlStatement = cmd.sql,
            arguments = cmd.args,
            success = { (_, resultSet) =>
              val res = extractResult(cmd, resultSet)
              stack match {
                case Nil => result = res
                case f :: tail => loop(f(Success(res)), tail)
              }
            },
            error = null // don't know how to handle errors/rollback on this level
          )
        case seq @ Sequence(_, _) => loop(flatten(seq), stack)
        case TransformWith(a, fA) => loop(a, fA.asInstanceOf[Try[_] => IO[_, _]] :: stack)
        case Transactional(t) => loop(t, stack)
      }
    }

    db.transaction { implicit tx =>
      loop(io, Nil)
    }.map { _ =>
      result.asInstanceOf[T]
    }
  }
}
