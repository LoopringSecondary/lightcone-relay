import sbt._

object Dependencies {

  lazy val slf4jVersion = "1.7.25"
  lazy val logbackVersion = "1.2.3"
  lazy val json4sVersion = "3.5.4"
  lazy val akkaVersion = "2.5.14"
  lazy val akkaHttpVersion = "10.1.3"
  lazy val slickVersion = "3.2.3"

  lazy val commonDependency = Seq(
    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "com.github.scopt" %% "scopt" % "3.7.0",
    "net.codingwell" %% "scala-guice" % "4.2.1",
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.0",
    "de.heikoseeberger" %% "akka-http-json4s" % "1.21.0",
    "org.web3j" % "core" % "3.4.0",
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "net.codingwell" %% "scala-guice" % "4.2.1",
    "ch.qos.logback" % "logback-core" % logbackVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    // "ch.qos.logback" % "logback-access" % logbackVersion,
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.json4s" %% "json4s-jackson" % json4sVersion,
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.json4s" %% "json4s-jackson" % json4sVersion,
    "org.json4s" %% "json4s-ext" % json4sVersion,
    "com.github.etaty" %% "rediscala" % "1.8.0",
    "com.github.nscala-time" %% "nscala-time" % "2.20.0",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.github.briandilley.jsonrpc4j" % "jsonrpc4j" % "1.5.3",
    "javax.servlet" % "javax.servlet-api" % "4.0.1",
    "io.spray" %% "spray-json" % "1.3.4")

  lazy val ethereumDependency = Seq(
    "org.ethereum" % "ethereumj-core" % "1.8.2-RELEASE")

  lazy val akkaDenepdencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion)

  lazy val socketIODenepdencies = Seq(
    "io.socket" % "socket.io-client" % "1.0.0" % Test,
    "org.reflections" % "reflections" % "0.9.11",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7",
    "com.corundumstudio.socketio" % "netty-socketio" % "1.7.16"
  )

  lazy val databaseDenepdencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "0.20",
    "org.mybatis" % "mybatis" % "3.4.6",
    "org.hsqldb" % "hsqldb" % "2.3.5",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.mybatis.scala" % "mybatis-scala-core_2.12" % "1.0.6-SNAPSHOT"
  )
}
