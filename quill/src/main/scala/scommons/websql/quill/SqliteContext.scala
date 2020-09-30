package scommons.websql.quill

import io.getquill.NamingStrategy
import scommons.websql.Database

class SqliteContext[T <: NamingStrategy](naming: T, db: Database)
  extends WebSqlContext(WebSqlDialect, naming, db)
    with SqliteEncoders
    with SqliteDecoders
