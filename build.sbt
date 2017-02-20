import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "pl.waw.oledzki",
      scalaVersion := "2.11.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Slowotok",

    libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.5.4",
    libraryDependencies += "com.typesafe.play" %% "play-logback" % "2.5.4",
    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3",
    libraryDependencies += "com.typesafe" % "config" % "1.3.1"
  )
