package definitions

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import scommons.sbtplugin.project.CommonModule.ideExcludedDirectories

import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._

trait ScalaJsModule extends WebSqlModule {

  override def definition: Project = {
    super.definition
      .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
      .settings(ScalaJsModule.settings: _*)
  }
}

object ScalaJsModule {

  val settings: Seq[Setting[_]] = Seq(
    scalaJSModuleKind := ModuleKind.CommonJSModule,

    //Opt-in @ScalaJSDefined by default
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    requireJsDomEnv in Test := false,
    version in webpack := "4.29.0",
    emitSourceMaps := false,
    webpackEmitSourceMaps := false,

    ideExcludedDirectories ++= {
      val base = baseDirectory.value
      List(
        base / "build",
        base / "node_modules"
      )
    }
  )
}
