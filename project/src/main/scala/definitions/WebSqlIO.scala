package definitions

import common.TestLibs
import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scoverage.ScoverageKeys.coverageExcludedPackages

object WebSqlIO extends ScalaJsModule {

  override val id: String = "scommons-websql-io"

  override val base: File = file("io")

  override def definition: Project = super.definition
    .settings(
      description := "High level WebSQL/SQLite IO monad API",

      coverageExcludedPackages := "scommons.websql.io.IOMonad",

      Test / npmDependencies ++= Seq(
        TestLibs.websql
      )
    )

  override val internalDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(
    WebSqlEncoding.definition
  )

  override val superRepoProjectsDependencies: Seq[(String, String, Option[String])] = Seq(
    ("scommons-nodejs", "scommons-nodejs-test", Some("test"))
  )

  override val runtimeDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Nil)

  override val testDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    TestLibs.scommonsNodejsTest.value
  ).map(_ % "test"))
}
