import sbt._
import Keys._

object Settings {
  lazy val basicSettings: Seq[Setting[_]] = Seq(
    name := "lightcone",
    organization := Globals.organization,
    version := Globals.version,
    scalaVersion := Globals.scalaVersion,
    autoScalaLibrary := false,
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    // resolvers ++= Resolvers.repositories,
    javacOptions := Seq( //"-source", Globals.jvmVersion,
    //"-target", Globals.jvmVersion
    ),
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-g:vars",
      "-unchecked",
      "-deprecation",
      "-Yresolve-term-conflict:package"),
    fork in Test := false,
    parallelExecution in Test := false,
    // publishArtifact in (Compile, packageSrc) := false,
    // publishArtifact in (Compile, packageDoc) := false,
    organizationName := "Loopring Foundation",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    shellPrompt in ThisBuild := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) })
}