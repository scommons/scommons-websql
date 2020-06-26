package scommons.websql.migrations

case class WebSqlMigration(version: Int,
                           name: String,
                           sql: String)
