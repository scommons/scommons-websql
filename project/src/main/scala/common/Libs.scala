package common

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._
import scommons.sbtplugin.project.CommonLibs

object Libs extends CommonLibs {

  val scommonsNodejsVersion = "0.3.0"

  lazy val quillSql = Def.setting("io.getquill" %%% "quill-sql" % "3.2.2")
}
