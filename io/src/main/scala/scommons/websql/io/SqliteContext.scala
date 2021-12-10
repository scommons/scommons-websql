package scommons.websql.io

import scommons.websql.Database
import scommons.websql.encoding.{SqliteDecoders, SqliteEncoders}

class SqliteContext(db: Database)
  extends WebSqlContext(db)
    with SqliteEncoders
    with SqliteDecoders
