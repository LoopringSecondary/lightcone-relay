import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val proto = (project in file("proto"))
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"),
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(
        flatPackage = false,
        javaConversions = true) -> (sourceManaged in Compile).value))

lazy val core = (project in file("core"))
  .dependsOn(proto)
  .settings(
    libraryDependencies ++= akkaDenepdencies)

lazy val lightcone = (project in file("."))
  .aggregate(proto, core)
  .settings(
    basicSettings,
    update / aggregate := false)