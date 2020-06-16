package definitions

import common.{Libs, TestLibs}
import sbt.Keys._
import sbt._

import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object WebSqlQuill extends ScalaJsModule {

  override val id: String = "scommons-websql-quill"

  override val base: File = file("quill")

  override def definition: Project = super.definition
    .settings(
      description := "quill bindings for WebSQL/SQLite Api",

      npmDependencies in Test ++= Seq(
        "websql" -> "1.0.0"
      )
    )

  override val internalDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(
    WebSqlCore.definition
  )

  override val superRepoProjectsDependencies: Seq[(String, String, Option[String])] = Seq(
    ("scommons-nodejs", "scommons-nodejs-test", Some("test"))
  )

  override val runtimeDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    Libs.quillSql.value
  ))

  override val testDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    TestLibs.scommonsNodejsTest.value
  ).map(_ % "test"))
}
