package definitions

import common.TestLibs
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys.coverageExcludedPackages

import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

object WebSqlCore extends ScalaJsModule {

  override val id: String = "scommons-websql-core"

  override val base: File = file("core")

  override def definition: Project = super.definition
    .settings(
      description := "Scala.js facade for WebSQL API",
      coverageExcludedPackages := "scommons.websql.raw",
      
      npmDependencies in Test ++= Seq(
        TestLibs.websql
      )
    )

  override val internalDependencies: Seq[ClasspathDep[ProjectReference]] = Nil

  override val superRepoProjectsDependencies: Seq[(String, String, Option[String])] = Seq(
    ("scommons-nodejs", "scommons-nodejs-test", Some("test"))
  )

  override val runtimeDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Nil)

  override val testDependencies: Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    TestLibs.scommonsNodejsTest.value
  ).map(_ % "test"))
}
