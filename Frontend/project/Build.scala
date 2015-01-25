import sbt._
import Keys._

object MyBuild extends Build {

    private val commonSettings = net.virtualvoid.sbt.graph.Plugin.graphSettings

    lazy val root = project
       .in(file("."))
       .settings(commonSettings:_*)
       .aggregate(core, util)

    lazy val core = project
      .settings(commonSettings:_*)
      .settings(
        libraryDependencies += "org.fluentd" % "fluent-logger" % "0.2.10"
      )

    lazy val util = project
      .settings(commonSettings:_*)
}
