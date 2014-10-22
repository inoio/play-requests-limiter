import play.PlayImport.PlayKeys

name := "play2-request-queue"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions += "-target:jvm-1.7"

// Common dependencies
val scalatest = "org.scalatest" %% "scalatest" % "2.2.0" % "test"
val scalatestPlusPlay = "org.scalatestplus" %% "play" % "1.2.0" % "test"
val mockito = "org.mockito" % "mockito-core" % "1.9.5" % "test"

//////////////////////////////////////////////////////
// Common Dependencies
//////////////////////////////////////////////////////
libraryDependencies ++= Seq (
  ws,
  scalatest,
  scalatestPlusPlay,
  mockito
)

lazy val main = project.in(file(".")).enablePlugins(PlayScala)
