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

  override type PrepareRow = Row
  override type ResultRow = Row

  override type Result[T] = T
  override type RunQueryResult[T] = Future[Seq[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Int]
  override type RunActionReturningResult[T] = Future[Int]
  override type RunBatchActionResult = Future[Seq[Int]]
  override type RunBatchActionReturningResult[T] = Future[Seq[Int]]

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

  def executeQuery[T](sql: String,
                      prepare: Prepare,
                      extractor: Extractor[T])(implicit tx: Transaction): Future[List[T]] = {
    
    val (_, values) = prepare(Row())
    
    executeSql(sql, values.data).map { resultSet =>
      resultSet.rows.map { row =>
        val obj = row.asInstanceOf[js.Dynamic]
        val res = js.Object.keys(row).map(obj.selectDynamic)
        extractor(Row(res: _*))
      }.toList
    }
  }

  def executeQuerySingle[T](sql: String,
                            prepare: Prepare,
                            extractor: Extractor[T])(implicit tx: Transaction): Future[T] = {
    
    executeQuery(sql, prepare, extractor).map(handleSingleResult)
  }

  def executeAction(sql: String, prepare: Prepare)(implicit tx: Transaction): Future[Int] = {
    val (_, values) = prepare(Row())

    executeSql(sql, values.data).map(_.rowsAffected)
  }

  def executeActionReturning(sql: String,
                             prepare: Prepare,
                             extractor: Extractor[Int],
                             returningColumn: String
                            )(implicit tx: Transaction): Future[Int] = {
    
    val (_, values) = prepare(Row())

    executeSql(sql, values.data).map(_.insertId.getOrElse(
      throw new IllegalStateException(s"insertId is required, but wasn't returned: $sql")
    ))
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit tx: Transaction): Future[Seq[Int]] = {
    Future.sequence(groups.map {
      case BatchGroup(sql, prepareList) =>
        Future.sequence(prepareList.map { prepare =>
          executeAction(sql, prepare)
        })
    }).map(_.flatten)
  }

  def executeBatchActionReturning(groups: List[BatchGroupReturning],
                                  extractor: Extractor[Int]
                                 )(implicit tx: Transaction): Future[List[Int]] = {

    Future.sequence(groups.map {
      case BatchGroupReturning(sql, column, prepareList) =>
        Future.sequence(prepareList.map { prepare =>
          executeActionReturning(sql, prepare, extractor, column)
        })
    }).map(_.flatten)
  }
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
