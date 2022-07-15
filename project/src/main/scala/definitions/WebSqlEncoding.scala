package definitions

import common.TestLibs
import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._
import scoverage.ScoverageKeys.coverageExcludedPackages

object WebSqlEncoding extends ScalaJsModule {

  override val id: String = "scommons-websql-encoding"

  override val base: File = file("encoding")

  override def definition: Project = super.definition
    .settings(
      description := "Encoders/Decoders for WebSQL/SQLite DB types",

      coverageExcludedPackages :=
        "scommons.websql.encoding.TupleEncoders" +
          ";scommons.websql.encoding.TupleOptDecoders" +
          ";scommons.websql.encoding.TupleDecoders",
      
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
