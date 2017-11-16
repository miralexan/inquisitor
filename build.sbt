lazy val inquisitor = (project in file(".")).
  settings(
    inThisBuild(List(
      name := "inquisitor",
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.2.0-SNAPSHOT",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
    )),

    libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.0" % Compile,
    libraryDependencies += "com.lucidchart" %% "xtract" % "1.1.1" % Compile,
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0" % Compile,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.24" % Compile,
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.16.1" % Compile,
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0" % Compile
  )
