package definitions

import common.TestLibs
import sbt.Keys._
import sbt._
import scommons.sbtplugin.ScommonsPlugin.autoImport._
import scoverage.ScoverageKeys.coverageExcludedPackages

import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object WebSqlMigrations extends ScalaJsModule {

  override val id: String = "scommons-websql-migrations"

  override val base: File = file("migrations")

  override def definition: Project = super.definition
    .settings(
      description := "Easy DB migrations for WebSQL/SQLite Api",
      coverageExcludedPackages := "scommons.websql.migrations.raw",

      scommonsBundlesFileFilter := "*.sql",

      Test / npmDependencies ++= Seq(
        TestLibs.websql
      )
    )

  override val internalDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(
    WebSqlCore.definition
  )

  override val superRepoProjectsDependencies: Seq[(String, String, Option[String])] = Seq(
    ("scommons-nodejs", "scommons-nodejs-test", Some("test"))
  )

  override val runtimeDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Nil)

  override val testDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    TestLibs.scommonsNodejsTest.value
  ).map(_ % "test"))
}
