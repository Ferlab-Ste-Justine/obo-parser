import sbt.Keys.version

name := "obo-parser"
scalaVersion := "2.11.12"
organization := "io.kf.etl"
version := "1.0.0"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")


resolvers ++= Seq(
)


libraryDependencies ++= Seq(
  spark_sql,
  scalatest % "test"
)


dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.5",
  "com.fasterxml.jackson.core" % "jackson-annotation" % "2.6.5",
  "org.json4s" %% "json4s-jackson" % "3.2.11"
)

test in assembly := {}
