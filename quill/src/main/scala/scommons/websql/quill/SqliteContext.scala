package scommons.websql.quill

import io.getquill.{NamingStrategy, SqliteDialect}
import scommons.websql.Database

class SqliteContext[T <: NamingStrategy](naming: T, db: Database)
  extends WebSqlContext(SqliteDialect, naming, db)
    with SqliteEncoders
    with SqliteDecoders
