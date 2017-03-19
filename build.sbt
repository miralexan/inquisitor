import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.2.0-SNAPSHOT"
    )),
    name := "Inquisitor",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.0",
    libraryDependencies += "com.lucidchart" %% "xtract" % "1.1.1",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.24",
    libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.16.1",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"
  )
