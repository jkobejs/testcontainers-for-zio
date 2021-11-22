val scala2Version              = "2.13.6"
val zioVersion                 = "1.0.12"
val zioMagicVersion            = "0.3.10"
val zioKafkaVersion            = "0.17.1"
val testcontainersScalaVersion = "0.39.12"
val mysqlConnnectorJVersion    = "8.0.27"
val slf4jVersion               = "1.7.32"
val logbackVersion             = "1.2.6"

ThisBuild / version       := "0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization  := "io.github.scottweaver"
ThisBuild / description   := "Provides ZIO ZLayer wrappers around Scala Testcontainers/"
ThisBuild / homepage      := Some(url("https://github.com/scottweaver/testcontainers-for-zio"))

ThisBuild / developers := List(
  Developer(
    id = "scottweaver",
    name = "Scott T Weaver",
    email = "scott.t.weaver@gmail.com",
    url = url("https://scottweaver.github.io/")
  )
)

commandAliases

lazy val models = project
  .in(file("modules/models"))
  .settings(settings)
  .settings(name := "zio-testcontainers-models")

// lazy val flyway         = project
//   .in(file("modules/flyway"))
//   .settings(settings)
//   .settings(name := "zio-flyway-aspects")

lazy val mysql           =
  project
    .in(file("modules/mysql"))
    .settings(settings)
    .settings(
      name := "zio-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val kafka           =
  project
    .in(file("modules/kafka"))
    .settings(settings)
    .settings(
      name := "zio-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % zioKafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % testcontainersScalaVersion
      )
    )

lazy val settings        =
  commonSettings ++
    publishSettings ++
    commandAliases

lazy val commonSettings  =
  Seq(
    scalaVersion := scala2Version,
    // Prevent slf4j 2.x from ruining EVERYTHING :(
    dependencyOverrides ++= Seq(
      "org.slf4j" % "slf4j-api" % slf4jVersion
    ),
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio"                       % zioVersion,
      "com.dimafeng"         %% "testcontainers-scala-core" % testcontainersScalaVersion,
      "dev.zio"              %% "zio-test"                  % zioVersion,
      "org.slf4j"             % "slf4j-api"                 % slf4jVersion,
      "ch.qos.logback"        % "logback-classic"           % logbackVersion  % Test,
      "io.github.kitlangton" %% "zio-magic"                 % zioMagicVersion % Test,
      "dev.zio"              %% "zio-test-sbt"              % zioVersion      % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / fork  := true
  )

lazy val publishSettings = Seq(
  pomIncludeRepository := { _ => false },
  publishTo            := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle    := true
)

lazy val commandAliases  =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck") ++
    addCommandAlias("publishAll", "models/publishSigned; mysql/publishSigned; kafka/publishSigned")

lazy val stdOptions      = Seq(
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-Xlint:_,-type-parameter-shadow,-byname-implicit",
  "-Xsource:2.13",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val stdOpts213      = Seq(
  "-Wunused:imports",
  "-Wvalue-discard",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:params"
)

ThisBuild / scalacOptions := stdOptions ++ stdOpts213
