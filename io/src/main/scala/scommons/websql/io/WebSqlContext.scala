package scommons.websql.io

import scommons.websql.encoding._
import scommons.websql._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Success, Try}

abstract class WebSqlContext(val db: Database)
  extends WebSqlEncoding
    with TupleEncoders
    with TupleOptDecoders
    with TupleDecoders
    with IOMonad {

  type Prepare = PrepareRow => (List[Any], PrepareRow)
  type Extractor[T] = ResultRow => T

  type RunQueryResult[T] = IO[Seq[T], Effect.Read]
  type RunQuerySingleResult[T] = IO[T, Effect.Read]
  type RunActionResult = IO[Long, Effect.Write]
  type RunActionReturningResult = IO[Long, Effect.Write]
  type RunBatchActionResult = IO[Seq[Long], Effect.Write]
  type RunBatchActionReturningResult = IO[Seq[Long], Effect.Write]

  private sealed trait SqlCommand[T] {
    def sql: String
    def args: Seq[js.Any]
  }
  private case class ExecQuery[T](sql: String, args: Seq[js.Any], extractor: Extractor[T]) extends SqlCommand[List[T]]
  private case class ExecAction(sql: String, args: Seq[js.Any]) extends SqlCommand[Long]
  private case class ExecActionReturning(sql: String, args: Seq[js.Any]) extends SqlCommand[Long]
  
  private case class Run[T, E <: Effect](cmd: SqlCommand[T]) extends IO[T, E]

  def runQuery[R, T](sql: String, extractor: R => T)
                    (implicit dec: Decoder[R]): RunQueryResult[T] = {
    
    executeQuery(sql, _ => (Nil, Nil), row => extractor(dec(0, row)))
  }

  def runQuery[P, R, T](sql: String, args: P, extractor: R => T)
                       (implicit enc: Encoder[P], dec: Decoder[R]): RunQueryResult[T] = {
    
    executeQuery(sql, row => (Nil, enc(-1, args, row)), row => extractor(dec(0, row)))
  }

  def runQuerySingle[R, T](sql: String, extractor: R => T)
                          (implicit dec: Decoder[R]): RunQuerySingleResult[T] = {
    
    executeQuerySingle(sql, _ => (Nil, Nil), row => extractor(dec(0, row)))
  }

  def runQuerySingle[P, R, T](sql: String, args: P, extractor: R => T)
                             (implicit enc: Encoder[P], dec: Decoder[R]): RunQuerySingleResult[T] = {
    
    executeQuerySingle(sql, row => (Nil, enc(-1, args, row)), row => extractor(dec(0, row)))
  }

  def runAction(sql: String): RunActionResult =
    executeAction(sql, _ => (Nil, Nil))

  def runAction[P](sql: String, args: P)(implicit enc: Encoder[P]): RunActionResult =
    executeAction(sql, row => (Nil, enc(-1, args, row)))

  def runActionReturning(sql: String): RunActionReturningResult =
    executeActionReturning(sql, _ => (Nil, Nil))

  def runActionReturning[P](sql: String, args: P)(implicit enc: Encoder[P]): RunActionReturningResult =
    executeActionReturning(sql, row => (Nil, enc(-1, args, row)))

  protected def executeQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T]): IO[List[T], Effect.Read] = {
    val (_, values) = prepare(Nil)
    Run(ExecQuery(sql, values, extractor))
  }

  protected def executeQuerySingle[T](sql: String, prepare: Prepare, extractor: Extractor[T]): IO[T, Effect.Read] = {
    executeQuery(sql, prepare, extractor).map(handleSingleResult)
  }

  protected def executeAction(sql: String, prepare: Prepare): IO[Long, Effect.Write] = {
    val (_, values) = prepare(Nil)
    Run(ExecAction(sql, values))
  }

  protected def executeActionReturning(sql: String, prepare: Prepare): IO[Long, Effect.Write] = {
    val (_, values) = prepare(Nil)
    Run(ExecActionReturning(sql, values))
  }

  protected def handleSingleResult[T](list: List[T]): T =
    list match {
      case value :: Nil => value
      case _ =>
        throw new IllegalStateException(s"Expected a single result but got ${list.size}")
    }

  def performIO[T, E <: Effect](io: IO[T, E]): Future[T] = {

    def flatten[Y, M[X] <: IterableOnce[X]](seq: Sequence[Y, M, Effect]): IO[M[Y], Effect] = {
      seq.in.iterator.foldLeft(IO.successful(seq.cbfResultToValue.newBuilder)) { (builder, item) =>
        builder.flatMap(b => item.map(b += _))
      }.map(_.result())
    }

    def extractResult[R](cmd: SqlCommand[R], resultSet: ResultSet): R = {
      val result = cmd match {
        case q: ExecQuery[_] => resultSet.rows.map(r => q.extractor(WebSqlRow(q.sql, r))).toList
        case ExecAction(_, _) => resultSet.rowsAffected
        case ExecActionReturning(sql, _) => resultSet.insertId.getOrElse(
          throw new IllegalStateException(s"insertId is required, but wasn't returned: $sql")
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
        case _ =>
          throw new IllegalStateException(s"Unknown IO: $io")
      }
    }

    db.transaction { implicit tx =>
      loop(io, Nil)
    }.map { _ =>
      result.asInstanceOf[T]
    }
  }
}
