package scommons.websql.raw

import scala.scalajs.js

/** @see https://www.w3.org/TR/webdatabase/
  * @see https://github.com/nolanlawson/node-websql/
  */
@js.native
trait WebSQLDatabase extends js.Object {
  
  val _db: WebSQLInternalDB = js.native

  def transaction(callback: js.Function1[WebSQLTransaction, Unit],
                  error: js.Function1[js.Error, Unit],
                  success: js.Function0[Unit]): Unit = js.native
}

@js.native
trait WebSQLTransaction extends js.Object {

  def executeSql(sqlStatement: String,
                 arguments: js.Array[js.Any],
                 success: js.Function2[WebSQLTransaction, WebSQLResultSet, Unit],
                 error: js.Function2[WebSQLTransaction, js.Error, Boolean]): Unit = js.native
}

@js.native
trait WebSQLResultSet extends js.Object {

  val insertId: js.UndefOr[Double]
  val rowsAffected: js.UndefOr[Double]
  val rows: js.UndefOr[WebSQLRows]
}

@js.native
trait WebSQLRows extends js.Object {

  val _array: js.Array[js.Object]
}
