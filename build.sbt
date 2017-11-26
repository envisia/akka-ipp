import ReleaseTransformations._
name := "akka-ipp"
organization := "de.envisia.akka"
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.12.4", "2.11.11" ,"2.13.0-M2")


lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Dependencies.commonDeps ++ Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  )

scalacOptions := Seq(
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

publishMavenStyle in ThisBuild := true
pomIncludeRepository in ThisBuild := { _ => false }
updateOptions := updateOptions.value.withGigahorse(false)
publishTo := Some("envisia-nexus" at "https://nexus.envisia.de/repository/internal/")

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)
