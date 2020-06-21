package scommons.websql.quill

import io.getquill.util.Messages

import scala.reflect.ClassTag
import scala.scalajs.js

class WebSqlRow(val data: js.Array[js.Any]) extends AnyVal {

  def apply[T](index: Int)(implicit t: ClassTag[T]): T = {
    data(index) match {
      case v: T => v
      case other => Messages.fail(
        s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'"
      )
    }
  }
}
