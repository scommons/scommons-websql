package scommons.websql.quill

import io.getquill._
import io.getquill.context.mirror.{MirrorEncoders, Row}
import io.getquill.context.sql.SqlContext
import io.getquill.idiom.Idiom
import scommons.websql.quill.WebSqlContext._
import scommons.websql.{Database, ResultSet, Transaction}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

class WebSqlContext[I <: Idiom, N <: NamingStrategy](val idiom: I,
                                                     val naming: N,
                                                     db: Database)
  extends SqlContext[I, N]
    with MirrorEncoders
    with WebSqlDecoders {
    //with ArrayMirrorEncoding
    //with SyncIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row

  override type Result[T] = T
  override type RunQueryResult[T] = Future[Seq[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = ActionMirror
  override type RunActionReturningResult[T] = ActionReturningMirror[T]
  override type RunBatchActionResult = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]

  override def close(): Unit = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: Transaction => Future[T]): Future[T] = {
    var resultF: Future[T] = null
    db.transaction { tx =>
      resultF = f(tx)
      setTransactionFinalized(tx)
    }.flatMap(_ => resultF)
  }

  case class ActionMirror(string: String, prepareRow: PrepareRow)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningColumn: String)

  case class BatchActionMirror(groups: List[(String, List[Row])])

  case class BatchActionReturningMirror[T](groups: List[(String, String, List[PrepareRow])], extractor: Extractor[T])

  def executeQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T])(implicit tx: Transaction): Future[List[T]] = {
    val (_, values) = prepare(Row())
    
    executeSql(sql, values.data).map { resultSet =>
      resultSet.rows.map { row =>
        val obj = row.asInstanceOf[js.Dynamic]
        val res = js.Object.keys(row).map(obj.selectDynamic)
        extractor(Row(res: _*))
      }.toList
    }
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare, extractor: Extractor[T])(implicit tx: Transaction): Future[T] = {
    executeQuery(sql, prepare, extractor).map(handleSingleResult)
  }

  def executeAction(string: String, prepare: Prepare = identityPrepare) = {
    val (_, values) = prepare(Row())
    val res = ActionMirror(string, values)
    println(s"executeAction: prepare: $prepare, res: $res")
    res
  }

  def executeActionReturning[O](string: String, prepare: Prepare, extractor: Extractor[O], returningColumn: String) =
    ActionReturningMirror[O](string, prepare(Row())._2, extractor, returningColumn)

  def executeBatchAction(groups: List[BatchGroup]) =
    BatchActionMirror {
      groups.map {
        case BatchGroup(string, prepare) =>
          (string, prepare.map(p => Row(p(Row())._2)))
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]) =
    BatchActionReturningMirror[T](
      groups.map {
        case BatchGroupReturning(string, column, prepare) =>
          (string, column, prepare.map(_ (Row())._2))
      }, extractor
    )
}
  
object WebSqlContext {

  private def setTransactionFinalized(tx: Transaction): Unit = {
    tx.asInstanceOf[js.Dynamic].updateDynamic("isFinalized")(true)
  }
  
  private def isTransactionFinalized(tx: Transaction): Boolean = {
    tx.asInstanceOf[js.Dynamic].selectDynamic("isFinalized")
      .asInstanceOf[js.UndefOr[Boolean]]
      .getOrElse(false)
  }
  
  private def executeSql(sql: String, args: Seq[Any])(implicit tx: Transaction): Future[ResultSet] = {
    val p = Promise[ResultSet]()

    if (isTransactionFinalized(tx)) p.failure(new IllegalStateException(
      "Transaction is already finalized. Use Future.sequence or run queries outside for-comprehension."
    ))
    else {
      tx.executeSql(
        sqlStatement = sql,
        arguments = js.Array(args.map(_.asInstanceOf[js.Any]): _*),
        success = { (_, resultSet) =>
          p.success(resultSet)
        },
        error = { (_, error) =>
          p.failure(js.JavaScriptException(error))
          true //rollback
        }
      )
    }

    p.future
  }
}
