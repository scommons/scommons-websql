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
  .aggregate(subProjects: _*)

lazy val subProjects = {
  val crossProjects = List[ProjectReference](
    `scommons-websql-core`,
    `scommons-websql-encoding`,
    `scommons-websql-io`,
    `scommons-websql-migrations`,
  )

  //TODO: quill-sql don't support Scala.js 1.1+ yet
  if (scalaJSVersion.startsWith("0.6")) {
    crossProjects :+ (`scommons-websql-quill`: ProjectReference)
  }
  else crossProjects
}

lazy val `scommons-websql-core` = WebSqlCore.definition
lazy val `scommons-websql-encoding` = WebSqlEncoding.definition
lazy val `scommons-websql-io` = WebSqlIO.definition
lazy val `scommons-websql-migrations` = WebSqlMigrations.definition
lazy val `scommons-websql-quill` = {
  if (scalaJSVersion.startsWith("0.6")) WebSqlQuill.definition
  else project in file("target")
}
