import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val proto = (project in file("proto"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = false) -> (sourceManaged in Compile).value))

lazy val lib = (project in file("lib"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(proto)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= ethereumDependency,
    libraryDependencies ++= socketIODenepdencies)

lazy val ethconn = (project in file("ethconn"))
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

lazy val gateway = (project in file("gateway"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
      basicSettings,
      libraryDependencies ++= commonDependency,
      libraryDependencies ++= akkaDenepdencies)

lazy val core = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(proto)
  .dependsOn(lib)
  .dependsOn(ethconn)
  .dependsOn(gateway)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDenepdencies)

lazy val lightcone = (project in file("."))
  .aggregate(proto, lib, ethconn, core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    basicSettings,
    update / aggregate := false)