package scommons.websql.quill

import io.getquill.SnakeCase

sealed class TestSqliteContext extends SqliteContext(SnakeCase)

object TestSqliteContext extends TestSqliteContext
