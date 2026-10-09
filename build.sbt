ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % "always"

val zioVersion        = "2.1.26"
val zioJsonVersion    = "0.9.2"
val tapirVersion      = "1.13.24"
val zioLoggingVersion = "2.5.3"
val zioConfigVersion  = "4.0.7"
val sttpVersion       = "3.11.0"
val javaMailVersion   = "1.6.2"
val stripeVersion     = "33.1.0"

val commonDependencies = Seq(
  "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"    % tapirVersion,
  "com.softwaremill.sttp.client3" %% "zio"               % sttpVersion,
  "dev.zio"                       %% "zio-json"          % zioJsonVersion
)

val serverDependencies = commonDependencies ++ Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-zio"                  % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"      % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"    % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"     % tapirVersion % "test",
  "dev.zio"                     %% "zio-logging"                % zioLoggingVersion,
  "dev.zio"                     %% "zio-logging-slf4j"          % zioLoggingVersion,
  "ch.qos.logback"               % "logback-classic"            % "1.5.36",
  "dev.zio"                     %% "zio-test"                   % zioVersion,
  "dev.zio"                     %% "zio-test-junit"             % zioVersion   % "test",
  "dev.zio"                     %% "zio-test-sbt"               % zioVersion   % "test",
  "dev.zio"                     %% "zio-test-magnolia"          % zioVersion   % "test",
  "dev.zio"                     %% "zio-mock"                   % "1.0.0-RC12" % "test",
  "dev.zio"                     %% "zio-config"                 % zioConfigVersion,
  "dev.zio"                     %% "zio-config-magnolia"        % zioConfigVersion,
  "dev.zio"                     %% "zio-config-typesafe"        % zioConfigVersion,
  "io.getquill"                 %% "quill-jdbc-zio"             % "4.8.6",
  "org.postgresql"               % "postgresql"                 % "42.7.11",
  "org.flywaydb"                 % "flyway-core"                % "12.9.0",
  "org.flywaydb"                 % "flyway-database-postgresql" % "12.9.0",
  "org.testcontainers"           % "testcontainers-postgresql"  % "2.0.5",
  // "io.github.scottweaver"         %% "zio-2-0-testcontainers-postgresql" % "0.10.0",
  "com.password4j" % "password4j"  % "1.8.4", // hash passwords using Argon2
  "dev.zio"       %% "zio-prelude" % "1.0.0-RC47",
  "com.auth0"      % "java-jwt"    % "4.5.2",
  "com.sun.mail"   % "javax.mail"  % javaMailVersion,
  "com.stripe"     % "stripe-java" % stripeVersion
)

libraryDependencySchemes += "dev.zio" %% "zio-json" % "always"

lazy val foundations = (project in file("modules/foundations"))
  .settings(
    libraryDependencies ++= serverDependencies
  )

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/common"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0" // implementations of java.time classes for Scala.JS,
    )
  )

lazy val server = (project in file("modules/server"))
  .settings(
    libraryDependencies ++= serverDependencies
  )
  .dependsOn(common.jvm)

lazy val app = (project in file("modules/app"))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %%% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir"   %%% "tapir-json-zio"    % tapirVersion,
      "com.softwaremill.sttp.client3" %%% "zio"               % sttpVersion,
      "dev.zio"                       %%% "zio-json"          % zioJsonVersion,
      "io.frontroute"                 %%% "frontroute"        % "0.18.1" // Brings in Laminar 16
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled               := true,
    autoAPIMappings                 := true,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass             := Some("com.rockthejvm.reviewboard.App")
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common.js)

lazy val root = (project in file("."))
  .settings(
    name := "zio-rite-of-passage"
  )
  .aggregate(server, app)
  .dependsOn(server, app)
