package scommons.websql.quill

import io.getquill.SnakeCase
import scommons.websql.Database

class TestSqliteContext(db: Database)
  extends SqliteContext(SnakeCase, db)
