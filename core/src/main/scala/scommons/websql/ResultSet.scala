package scommons.websql

import scala.scalajs.js

class ResultSet(underlying: raw.WebSQLResultSet) {

  lazy val insertId: Option[Int] = underlying.insertId.toOption
  lazy val rowsAffected: Int = underlying.rowsAffected.getOrElse(0)
  lazy val rows: List[js.Object] = underlying.rows.map(_._array.toList).getOrElse(Nil)
}
