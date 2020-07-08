package scommons.websql.migrations.raw

import scala.scalajs.js

@js.native
trait WebSqlMigrationBundle extends js.Array[WebSqlMigrationBundleItem]

@js.native
trait WebSqlMigrationBundleItem extends js.Object {
  
  def file: String = js.native
  def content: String = js.native
}
