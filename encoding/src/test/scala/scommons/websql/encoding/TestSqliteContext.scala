package scommons.websql.encoding

import scommons.websql.Database

class TestSqliteContext(val db: Database)
  extends WebSqlEncoding
    with TupleEncoders
    with TupleDecoders
    with SqliteEncoders
    with SqliteDecoders
