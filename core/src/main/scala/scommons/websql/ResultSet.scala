package scommons.websql

import scala.scalajs.js

class ResultSet(underlying: raw.WebSQLResultSet) {

  lazy val insertId: Option[Long] = underlying.insertId.map(_.toLong).toOption
  lazy val rowsAffected: Long = underlying.rowsAffected.map(_.toLong).getOrElse(0L)
  
  lazy val rows: Seq[js.Object] =
    underlying.rows.map(_._array.toSeq).getOrElse(Seq.empty[js.Object])
}
