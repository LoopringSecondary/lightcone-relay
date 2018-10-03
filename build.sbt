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

lazy val ethconn = (project in file("ethconn"))
  // .enablePlugins(DockerPlugin)
  // .enablePlugins(JavaAppPackaging)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDenepdencies,
    libraryDependencies ++= ethereumDependency)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen(
        flatPackage = false) -> (sourceManaged in Compile).value))

lazy val core = (project in file("core"))
  .dependsOn(proto)
  .dependsOn(lib)
  .dependsOn(ethconn)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDenepdencies,
    libraryDependencies ++= Seq(
      "net.codingwell" %% "scala-guice" % "4.2.1"))

lazy val lightcone = (project in file("."))
  .aggregate(proto, lib, ethconn, core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    update / aggregate := false)