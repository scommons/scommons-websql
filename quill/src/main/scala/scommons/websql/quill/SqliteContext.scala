package scommons.websql.quill

import io.getquill._

class SqliteContext[T <: NamingStrategy](naming: T)
  extends SqlMirrorContext(SqliteDialect, naming) {

}
