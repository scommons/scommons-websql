package common

import common.Libs._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._
import scommons.sbtplugin.project.CommonTestLibs

object TestLibs extends CommonTestLibs {

  lazy val scommonsNodejsTest = Def.setting("org.scommons.nodejs" %%% "scommons-nodejs-test" % scommonsNodejsVersion)
  
  //npmDependencies
  lazy val websql: (String, String) = "websql" -> "2.0.3"
}
