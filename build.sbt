

name := "obo-parser"
scalaVersion := "2.12.12"
organization := "bio.ferlab"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
val spark_version = "3.4.2"
val deltaCoreVersion = "2.4.0"
/* Runtime */
libraryDependencies += "org.apache.spark" %% "spark-sql" % spark_version % Provided
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "3.3.6" % Provided
libraryDependencies += "io.delta" %% "delta-core" % deltaCoreVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.15.0"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "5.0.0"
/* Test */
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test"
libraryDependencies += "org.apache.spark" %% "spark-hive" % spark_version % "test"
assembly / test := {}

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("shapeless.**" -> "shadeshapless.@1").inAll
)

assembly / assemblyMergeStrategy := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  case "META-INF/native/libnetty_transport_native_epoll_x86_64.so" => MergeStrategy.last
  case "META-INF/DISCLAIMER" => MergeStrategy.last
  case "mozilla/public-suffix-list.txt" => MergeStrategy.last
  case "overview.html" => MergeStrategy.last
  case "git.properties" => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard
  case "mime.types" => MergeStrategy.first
  case PathList("scala", "annotation", "nowarn.class" | "nowarn$.class") => MergeStrategy.first
  case x if x.contains("org/apache/batik/") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
assembly / assemblyJarName := "obo-parser.jar"