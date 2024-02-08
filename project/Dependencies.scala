import sbt.*

object Dependencies {

  val pekkoHttpV = "1.0.0"
  val uTestV    = "0.8.2"
  val pekkoV     = "1.0.2"
  val slf4jV    = "2.0.9"
  val tikaV     = "2.8.0"

  lazy val commonDeps: Seq[ModuleID] =
    Seq(
      "com.lihaoyi"       %% "utest"               % uTestV % Test,
      "org.apache.pekko" %% "pekko-actor"          % pekkoV,
      "org.apache.pekko" %% "pekko-slf4j"          % pekkoV,
      "org.apache.pekko" %% "pekko-stream"         % pekkoV,
      "org.apache.pekko" %% "pekko-http"           % pekkoHttpV,
      "org.apache.pekko" %% "pekko-http-testkit"   % pekkoHttpV % Test,
      "org.apache.pekko" %% "pekko-stream-testkit" % pekkoV % Test,
      "org.slf4j"         % "slf4j-api"            % slf4jV,
      "org.slf4j"         % "slf4j-simple"         % slf4jV % Test,
      "org.apache.tika"   % "tika-core"            % tikaV % Test,
      "org.apache.tika"   % "tika-parsers"         % tikaV % Test,
      "org.apache.tika"   % "tika-parsers-standard-package" % tikaV % Test
    )

}
