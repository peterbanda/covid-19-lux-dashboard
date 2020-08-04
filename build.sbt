import PlayKeys._

organization := "com.bnd"

name := "covid-19-lux-dashboard"

version := "0.2.1"

scalaVersion := "2.11.12"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

libraryDependencies ++= Seq(cache, ws, filters)

PlayKeys.devSettings := Seq(
  "play.server.netty.maxInitialLineLength" -> "16384",
  "play.server.netty.transport" -> "jdk"
)

resolvers ++= Seq(
  Resolver.mavenLocal
)

routesImport ++= Seq(
  "reactivemongo.bson.BSONObjectID",
  "org.ada.web.controllers.PathBindables._",
  "org.ada.web.controllers.QueryStringBinders._"
)

val playVersion = "2.5.9"

libraryDependencies ++= Seq(
  "org.adada" %% "ada-web" % "0.9.0-200726",
  "org.adada" %% "ada-web" % "0.9.0-200726" classifier "assets",
  "com.bnd-lib" %% "scala-basecamp-3-client" % "0.0.3",                   // Basecamp 3 API client
  "org.apache.poi" % "poi-ooxml" % "4.0.1",                               // Read Excel
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.apache.pdfbox" % "pdfbox" % "2.0.1"
).map(_.exclude("org.slf4j", "slf4j-log4j12"))

val jacksonVersion = "2.8.8"

// Because of Spark
dependencyOverrides ++= Set(
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion
)

pipelineStages in Assets := Seq(digest, gzip) // closure, cssCompress

excludeFilter in gzip := (excludeFilter in gzip).value || new SimpleFileFilter(file => new File(file.getAbsolutePath + ".gz").exists)

includeFilter in digest := (includeFilter in digest).value && new SimpleFileFilter(f => f.getPath.contains("public/"))