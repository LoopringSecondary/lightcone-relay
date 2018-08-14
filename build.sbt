import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val data = (project in file("data"))
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"),
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(
        flatPackage = false,
        javaConversions = true) -> (sourceManaged in Compile).value))

// lazy val lib = (project in file("lib"))
//   .dependsOn(data)
//   .enablePlugins(AutomateHeaderPlugin)
//   .settings(
//     basicSettings,
//     libraryDependencies ++= commonDependency,
//     libraryDependencies ++= ethereumDependency)

lazy val core = (project in file("core"))
  .dependsOn(data)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDenepdencies)

lazy val lightcone = (project in file("."))
  .aggregate(data, core)
  .settings(
    basicSettings,
    update / aggregate := false)