import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val proto = (project in file("proto"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"),
    PB.targets in Compile := Seq(
      // PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(
        // javaConversions = true,
        flatPackage = false) -> (sourceManaged in Compile).value))

lazy val lib = (project in file("lib"))
  .dependsOn(proto)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= ethereumDependency)

lazy val core = (project in file("core"))
  .dependsOn(proto)
  // .dependsOn(lib)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDenepdencies)

lazy val lightcone = (project in file("."))
  .aggregate(proto, core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    update / aggregate := false)