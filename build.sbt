ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "org.loopring"

val akkaVer = "2.5.14"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

val ethereumDependencies = Seq(
  "org.web3j" % "core" % "3.4.0"
)

val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-remote" % akkaVer,
    "com.typesafe.akka" %% "akka-stream" % akkaVer,
    "com.typesafe.akka" %% "akka-cluster" % akkaVer,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVer,
    "com.typesafe.akka" %% "akka-persistence" % akkaVer,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVer,
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVer,
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "0.14.0",
    "com.lightbend.akka.discovery" %% "akka-discovery-dns" % "0.14.0",
    "com.typesafe.akka" %% "akka-http" % "10.1.3",
    "com.typesafe.akka" %% "akka-stream" % "2.5.13",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.3" % Test
)

val scalaPbDependencies = Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.0",
)
lazy val lightcone = (project in file("."))
  .settings(
    name := "lightcone",
  )
  .aggregate(data)
  .aggregate(core)

lazy val data = (project in file("data"))
  .settings(
    name := "lightcone-data",
  )

lazy val core = (project in file("core"))
  .dependsOn(data)
  .settings(
    name := "lightcone-core",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= scalaPbDependencies,
    libraryDependencies ++= ethereumDependencies,
  )

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value,
  PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
)