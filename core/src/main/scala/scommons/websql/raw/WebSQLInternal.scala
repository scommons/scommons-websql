package scommons.websql.raw

import scala.scalajs.js

/**
  * @see https://github.com/nolanlawson/node-websql/blob/master/lib/sqlite/SQLiteDatabase.js
  */
@js.native
trait WebSQLInternalDB extends js.Object {

  def exec(queries: js.Array[WebSQLInternalQuery],
           readOnly: Boolean,
           callback: js.Function2[js.Error, js.Array[WebSQLInternalResult], Unit]
          ): Unit = js.native
}

trait WebSQLInternalQuery extends js.Object {

  val sql: String
  val args: js.Array[js.Any]
}

@js.native
trait WebSQLInternalResult extends js.Object {

  val error: js.UndefOr[js.Error]
  val insertId: js.UndefOr[Double]
  val rowsAffected: js.UndefOr[Double]
  val rows: js.UndefOr[js.Array[js.Dynamic]]
}
