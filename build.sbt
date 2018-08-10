import ReleaseTransformations._
import xerial.sbt.Sonatype.GitHubHosting

updateOptions := updateOptions.value.withGigahorse(false)

name := "akka-ipp"
organization in ThisBuild := "de.envisia.ipp"
scalaVersion in ThisBuild := "2.12.6"
crossScalaVersions in ThisBuild := Seq("2.12.6", "2.11.11")
testFrameworks += new TestFramework("utest.runner.Framework")
sonatypeProfileName := "de.envisia.ipp"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Dependencies.commonDeps ++ Seq(
      scalaOrganization.value % "scala-reflect" % scalaVersion.value
    )
  )

scalacOptions := Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

// Sonatype Settings for Publishing
developers := List(
  Developer(
    id = "schmitch",
    name = "Christian Schmitt",
    email = "c.schmitt@envisia.de",
    url = url("http://github.com/schmitch")
  ),
  Developer(
    id = "zy4",
    name = "Seung-Zin Nam",
    email = "z.nam@envisia.de",
    url = url("http://github.com/zy4")
  )
)
sonatypeProjectHosting := Some(GitHubHosting("envisia", "akka-ipp", "c.schmitt@envisia.de"))
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
publishMavenStyle in ThisBuild := true
pomIncludeRepository in ThisBuild := { _ =>
  false
}
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
// Release Settings
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
