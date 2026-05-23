ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "plagiarism"


lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  IntegrationTest / fork := true,
  IntegrationTest / parallelExecution := false
)


val scalaTestVersion        = "3.2.19"
val testcontainersVersion   = "0.43.0"
val qdrantClientVersion     = "1.14.1"
val djlVersion              = "0.31.1"
val caskVersion             = "0.9.7"
val scalatagsVersion        = "0.13.1"
val javaDiffUtilsVersion    = "4.12"
val requestsVersion         = "0.9.0"
val slf4jVersion            = "2.0.16"
val logbackVersion          = "1.5.12"


lazy val root = (project in file("."))
  .aggregate(core, vectordb, webApp, testUtils)
  .settings(
    name := "plagiarism-checker",
    commonSettings,
    publish / skip := true
  )

lazy val testUtils = (project in file("test-utils"))
  .settings(
    name := "test-utils",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.scalatest"  %% "scalatest"                       % scalaTestVersion,
      "com.dimafeng"   %% "testcontainers-scala-scalatest"  % testcontainersVersion,
      "com.dimafeng"   %% "testcontainers-scala-quadrant"   % testcontainersVersion,
      "io.qdrant"       % "client"                          % qdrantClientVersion,
      "org.slf4j"       % "slf4j-api"                       % slf4jVersion,
      "ch.qos.logback"  % "logback-classic"                 % logbackVersion,
    )
  )

lazy val core = (project in file("core"))
  .configs(IntegrationTest)
  .settings(
    name := "core",
    commonSettings,
    itSettings,
    libraryDependencies ++= Seq(
      "io.github.java-diff-utils" % "java-diff-utils" % javaDiffUtilsVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
    ),
  )
  .dependsOn(testUtils % "it,test")

lazy val vectordb = (project in file("vectordb"))
  .configs(IntegrationTest)
  .dependsOn(core)
  .settings(
    name := "vectordb",
    commonSettings,
    itSettings,
    libraryDependencies ++= Seq(
      // Qdrant
      "io.qdrant" % "client" % qdrantClientVersion,
      // DJL + PyTorch
      "ai.djl"              % "api"               % djlVersion,
      "ai.djl"              % "model-zoo"          % djlVersion,
      "ai.djl.pytorch"      % "pytorch-engine"    % djlVersion,
      "ai.djl.huggingface"  % "tokenizers"         % djlVersion,
      // Logging
      "org.slf4j"      % "slf4j-api"        % slf4jVersion,
      "ch.qos.logback" % "logback-classic"  % logbackVersion,
      // Test
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
    ),
  )
  .dependsOn(testUtils % "it,test")

lazy val webApp = (project in file("web-app"))
  .configs(IntegrationTest)
  .dependsOn(core, vectordb)
  .settings(
    name := "web-app",
    commonSettings,
    itSettings,
    run / fork := true,
    run / connectInput := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"      % caskVersion,
      "com.lihaoyi"   %% "scalatags" % scalatagsVersion,
      "com.lihaoyi"   %% "requests"  % requestsVersion,
      // Logging
      "org.slf4j"      % "slf4j-api"       % slf4jVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Test
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
    ),
  )
  .dependsOn(testUtils % "it,test")


addCommandAlias("unitTest", "test")
addCommandAlias("itTest", "IntegrationTest/test")
addCommandAlias("allTest", ";test;IntegrationTest/test")
