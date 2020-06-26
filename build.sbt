import definitions._
import scommons.sbtplugin.project.CommonModule
import scommons.sbtplugin.project.CommonModule.ideExcludedDirectories

lazy val `scommons-websql` = (project in file("."))
  .settings(CommonModule.settings: _*)
  .settings(WebSqlModule.settings: _*)
  .settings(
    skip in publish := true,
    publish := ((): Unit),
    publishLocal := ((): Unit),
    publishM2 := ((): Unit)
  )
  .settings(
    ideExcludedDirectories += baseDirectory.value / "docs" / "_site"
  )
  .aggregate(
  `scommons-websql-core`,
  `scommons-websql-migrations`,
  `scommons-websql-quill`
)

lazy val `scommons-websql-core` = WebSqlCore.definition
lazy val `scommons-websql-migrations` = WebSqlMigrations.definition
lazy val `scommons-websql-quill` = WebSqlQuill.definition
