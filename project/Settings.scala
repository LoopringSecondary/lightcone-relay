import sbt._
import Keys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.MappingsHelper._
import xerial.sbt.Sonatype.SonatypeKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.SbtScalariform.autoImport._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object Settings {
  lazy val basicSettings: Seq[Setting[_]] = Seq(
    organization := Globals.organization,
    version := Globals.version,
    scalaVersion := Globals.scalaVersion,
    autoScalaLibrary := false,
    resolvers += "mvnrepository" at "http://mvnrepository.com/artifact/",
    resolvers += "ethereumlibrepository" at "https://dl.bintray.com/ethereum/maven/",
    resolvers += "JFrog" at "https://oss.jfrog.org/libs-release/",
    resolvers += "bintray" at "https://dl.bintray.com/ethereum/maven/",
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    javacOptions := Seq( //"-source", Globals.jvmVersion,
    ),
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-g:vars",
      "-unchecked",
      "-deprecation",
      "-Yresolve-term-conflict:package"),
    fork in Test := false,
    parallelExecution in Test := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageDoc) := false,
    organizationName := "Loopring Foundation",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    shellPrompt in ThisBuild := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) },
    publishTo := sonatypePublishTo.value,
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges),
    scalariformAutoformat := true,
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 20)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(NewlineAtEndOfFile, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(DanglingCloseParenthesis, Force)
      .setPreference(FirstArgumentOnNewline, Force)
      .setPreference(AllowParamGroupsOnNewlines, true))
}
