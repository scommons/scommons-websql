package scommons.websql

import scala.reflect.ClassTag
import scala.scalajs.js

class WebSqlRow private(sql: String,
                        columns: js.Array[String],
                        val data: js.Array[js.Any]) {

  private var nextIndex = 0
  
  def index: Int = nextIndex
  
  def isDefinedAt(index: Int): Boolean = {
    val v = data(index)
    v != null && !js.isUndefined(v)
  }
  
  def skipIndices(n: Int): Unit = {
    nextIndex += n
  }

  def apply[T](index: Int)(implicit t: ClassTag[T]): T = {
    nextIndex = index + 1

    data(index) match {
      case v: T => v
      case other =>
        val column = columns(index)
        throw new IllegalStateException(
          s"Expected '${t.runtimeClass}' type, but got '$other'\n\tat column: '$column', sql: $sql"
        )
    }
  }
}

object WebSqlRow {

  def apply(sql: String, row: js.Dynamic): WebSqlRow = {
    val columns = js.Object.keys(row.asInstanceOf[js.Object])
    val values = columns.map(k => row.selectDynamic(k).asInstanceOf[js.Any])

    new WebSqlRow(sql, columns, values)
  }
}
