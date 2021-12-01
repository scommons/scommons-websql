package scommons.websql

import scala.reflect.ClassTag
import scala.scalajs.js

class WebSqlRow private(val data: js.Array[js.Any]) extends AnyVal {

  def apply[T](index: Int)(implicit t: ClassTag[T]): T = {
    data(index) match {
      case v: T => v
      case other =>
        throw new IllegalStateException(s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'")
    }
  }
}

object WebSqlRow {

  def apply(row: js.Dynamic): WebSqlRow = {
    val res = js.Object.keys(row.asInstanceOf[js.Object])
      .map(k => row.selectDynamic(k).asInstanceOf[js.Any])

    new WebSqlRow(res)
  }
}
