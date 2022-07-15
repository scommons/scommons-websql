import definitions._
import scommons.sbtplugin.project.CommonModule
import scommons.sbtplugin.project.CommonModule.ideExcludedDirectories

lazy val `scommons-websql` = (project in file("."))
  .settings(CommonModule.settings: _*)
  .settings(WebSqlModule.settings: _*)
  .settings(
    publish / skip := true,
    publish := ((): Unit),
    publishLocal := ((): Unit),
    publishM2 := ((): Unit)
  )
  .settings(
    ideExcludedDirectories += baseDirectory.value / "docs" / "_site"
  )
  .aggregate(
    `scommons-websql-core`,
    `scommons-websql-encoding`,
    `scommons-websql-io`,
    `scommons-websql-migrations`
    //`scommons-websql-quill`
  )

lazy val `scommons-websql-core` = WebSqlCore.definition
lazy val `scommons-websql-encoding` = WebSqlEncoding.definition
lazy val `scommons-websql-io` = WebSqlIO.definition
lazy val `scommons-websql-migrations` = WebSqlMigrations.definition

//TODO: quill-sql don't support Scala.js 1.1+ yet
//  see: https://github.com/zio/zio-quill/issues/2175
//lazy val `scommons-websql-quill` = WebSqlQuill.definition
