import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.ModuleSplitStyle
import smithy4s.codegen.Smithy4sCodegenPlugin
import scala.sys.process.Process
import java.nio.file.*
import java.nio.charset.StandardCharsets

Compile / run / fork          := true
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalacOptions := Seq(
  "-Wunused:all"
)
inThisBuild(
  Seq(
    semanticdbEnabled := true, // for scalafix
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

resolvers +=
  "Sonatype S01 OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

val Versions = new {
  val ciris             = "3.5.0"
  val iron              = "2.4.0"
  val logback           = "1.4.14"
  val redis4cats        = "1.5.1"
  val scalaCheck        = "1.17.0"
  val doobie            = "1.0.0-RC4"
  val doobieTypeSafe    = "0.1.0"
  val log4cats          = "2.6.0"
  val http4sBlaze       = "0.23.14"
  val http4s            = "0.23.18"
  val Scala             = "3.5.0"
  val jwt               = "9.1.2"
  val Flyway            = "10.7.2"
  val Postgres          = "42.6.0"
  val TestContainers    = "0.41.4"
  val Weaver            = "0.8.3"
  val WeaverPlaywright  = "0.0.5"
  val Laminar           = "15.0.1"
  val waypoint          = "6.0.0"
  val monocle           = "3.2.0"
  val circe             = "0.14.3"
  val macroTaskExecutor = "1.1.1"
  val cats              = "2.10.0"
  val password4j        = "1.7.3"
  val upickle           = "4.0.1"
}

val Config = new {
  val DockerImageName = "miueon/realworld-smithy4s"
  val DockerBaseImage = "wonder/jdk:17.0.10_7-ubuntu"
  val BasePackage     = "realworld"
}

lazy val root = project
  .in(file("."))
  .aggregate(backend.projectRefs*)
  .aggregate(shared.projectRefs*)
  .aggregate(frontend.projectRefs*)

lazy val app = projectMatrix
  .in(file("modules/app"))
  .dependsOn(backend)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion              := Versions.Scala,
    Compile / doc / sources   := Seq.empty,
    dockerBaseImage           := Config.DockerBaseImage,
    dockerUpdateLatest        := true,
    Docker / packageName      := Config.DockerImageName,
    Docker / dockerRepository := Some("ghcr.io"),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % Versions.http4sBlaze,
      "org.http4s"    %% "http4s-ember-server" % Versions.http4s,
      "org.postgresql" % "postgresql"          % Versions.Postgres,
      "ch.qos.logback" % "logback-classic"     % Versions.logback
    ),
    reStart / baseDirectory := (ThisBuild / baseDirectory).value,
    run / baseDirectory     := (ThisBuild / baseDirectory).value
  )

def copyAll(location: File, outDir: File) = {
  IO.listFiles(location).toList.map { file =>
    val (name, ext) = file.baseAndExt
    val out         = outDir / (name + "." + ext)
    IO.copyFile(file, out)
    out
  }
}

val iron = Seq(
  "io.github.iltotore" %% "iron"       % Versions.iron,
  "io.github.iltotore" %% "iron-cats"  % Versions.iron,
  "io.github.iltotore" %% "iron-circe" % Versions.iron,
  "io.github.iltotore" %% "iron-ciris" % Versions.iron
)

val db = Seq(
  "org.flywaydb"        % "flyway-database-postgresql" % Versions.Flyway,
  "org.tpolecat"       %% "doobie-postgres"            % Versions.doobie,
  "org.tpolecat"       %% "doobie-hikari"              % Versions.doobie,
  "io.github.arturaz"  %% "doobie-typesafe"            % Versions.doobieTypeSafe,
  "io.github.iltotore" %% "iron-doobie"                % Versions.iron
)

lazy val backend = projectMatrix
  .in(file("modules/backend"))
  .dependsOn(shared)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .settings(
    scalaVersion            := Versions.Scala,
    Compile / doc / sources := Seq.empty,
    libraryDependencies ++= Seq(
      ("com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % smithy4sVersion.value),
      "com.disneystreaming.smithy4s"  %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "com.github.jwt-scala"          %% "jwt-circe"               % Versions.jwt,
      "com.password4j"                 % "password4j"              % Versions.password4j,
      "dev.optics"                    %% "monocle-core"            % Versions.monocle,
      "dev.profunktor"                %% "redis4cats-effects"      % Versions.redis4cats,
      "dev.profunktor"                %% "redis4cats-log4cats"     % Versions.redis4cats,
      "org.typelevel"                 %% "log4cats-slf4j"          % Versions.log4cats,
      "is.cir"                        %% "ciris"                   % Versions.ciris,
      "is.cir"                        %% "ciris-refined"           % Versions.ciris
    ),
    libraryDependencies ++= db,
    libraryDependencies ++= iron,
    libraryDependencies ++=
      Seq(
        "org.typelevel" %% "cats-core"                       % Versions.cats,
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % Versions.TestContainers,
        "com.dimafeng"  %% "testcontainers-scala-redis"      % Versions.TestContainers,
        "com.indoorvivants.playwright" %% "weaver"              % Versions.WeaverPlaywright,
        "com.disneystreaming"          %% "weaver-cats"         % Versions.Weaver,
        "com.disneystreaming"          %% "weaver-scalacheck"   % Versions.Weaver,
        "org.http4s"                   %% "http4s-blaze-server" % Versions.http4sBlaze,
        "org.http4s"                   %% "http4s-blaze-client" % Versions.http4sBlaze,
        "org.http4s"                   %% "http4s-ember-server" % Versions.http4s,
        "org.http4s"                   %% "http4s-ember-client" % Versions.http4s,
        "org.typelevel"                %% "log4cats-noop"       % Versions.log4cats
      ).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork          := true,
    Test / baseDirectory := (ThisBuild / baseDirectory).value
  )

lazy val shared = projectMatrix
  .in(file("modules/shared"))
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.Scala))
  .jsPlatform(Seq(Versions.Scala))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    smithy4sWildcardArgument := "?",
    libraryDependencies ++= Seq(
      "io.github.iltotore"           %%% "iron"          % Versions.iron,
      "io.github.iltotore"           %%% "iron-cats"     % Versions.iron,
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
    ),
    Compile / doc / sources := Seq.empty
  )

lazy val frontend = projectMatrix
  .in(file("modules/frontend"))
  .defaultAxes((defaults :+ VirtualAxis.js)*)
  .dependsOn(shared)
  .jsPlatform(Seq(Versions.Scala))
  .enablePlugins(ScalaJSPlugin, BundleMonPlugin)
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      import org.scalajs.linker.interface.ModuleSplitStyle
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("realworld")))
    },
    externalNpm := (ThisBuild / baseDirectory).value / "modules" / "frontend",
    libraryDependencies ++= Seq(
      ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0")
        .cross(CrossVersion.for3Use2_13),
      "dev.optics"         %%% "monocle-core"                % Versions.monocle,
      "com.raquo"          %%% "waypoint"                    % Versions.waypoint,
      "com.raquo"          %%% "laminar"                     % Versions.Laminar,
      "org.scala-js"       %%% "scala-js-macrotask-executor" % Versions.macroTaskExecutor,
      "io.laminext"        %%% "validation-cats"             % "0.15.0",
      "tech.neander"       %%% "smithy4s-fetch"              % "0.0.4",
      "com.lihaoyi"        %%% "upickle"                     % Versions.upickle,
      "io.github.iltotore" %%% "iron-upickle"                % Versions.iron
    ),
    watchSources := watchSources.value.filterNot { source =>
      source.base.getName.endsWith(".less") || source.base.getName.endsWith(".css")
    },
    stIgnore ++= List(
      "bootstrap-icons",
      "autoprefixer",
      "less",
      "terser",
      "glob",
      "vite",
      "rollup-plugin-copy",
      "rollup-plugin-sourcemaps",
      "@shoelace-style/shoelace",
      "@scala-js/vite-plutin-scalaja",
      "@raquo/vite-plugin-import-side-effect",
      "@raquo/vite-plugin-glob-resolver",
      "postcess",
      "tailwindcss"
    ),
    stIncludeDev := true
  )

lazy val defaults =
  Seq(VirtualAxis.scalaABIVersion(Versions.Scala), VirtualAxis.jvm)

lazy val isRelease = sys.env.get("RELEASE").contains("yesh")

val buildFrontend = taskKey[Unit]("Build frontend")

buildFrontend := {
  def frontendProj    = frontend.finder(VirtualAxis.js)(Versions.Scala)
  val frontendDirPath = frontend.base.getAbsolutePath()
  val appDirPath      = app.base.getAbsolutePath()

  (if (isRelease) {
     (frontendProj / Compile / fullLinkJS)
   } else {
     (frontendProj / Compile / fastLinkJS)
   }).value

  // Install JS dependencies from package-lock.json
  val npmCiExitCode = Process("pnpm install --frozen-lockfile", cwd = frontend.base).!
  if (npmCiExitCode > 0) {
    throw new IllegalStateException(s"pnpm ci failed. See above for reason")
  }

  // Build the frontend with vite
  val buildExitCode = Process("pnpm build", cwd = frontend.base).!
  if (buildExitCode > 0) {
    throw new IllegalStateException(s"Building frontend failed. See above for reason")
  }

  IO.delete(app.base / "src" / "main" / "resources" / "static")
  IO.copyDirectory(
    source = frontend.base / "dist",
    target = app.base / "src" / "main" / "resources" / "static"
  )

  val log = streams.value.log
  def processFile(file: Path): Unit = {
    val content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    val updatedContent = content.replaceAll(
      """(<script[^>]*\s+src=")([^"]+)""",
      s"""$$1/static$$2"""
    )
    Files.write(file, updatedContent.getBytes(StandardCharsets.UTF_8))
    log.info(s"Updated ${file.getFileName}")
  }

  processFile(Paths.get(appDirPath, "src", "main", "resources", "static", "index.html"))
}

(app.finder(VirtualAxis.jvm)(Versions.Scala) / Docker / publishLocal) := (app.finder(
  VirtualAxis.jvm
)(Versions.Scala) / Docker / publishLocal).dependsOn(buildFrontend).value

addCommandAlias("publishDockerLocal", "app/Docker/publishLocal")
addCommandAlias("publishDocker", "app/Docker/publish")
addCommandAlias("stubTests", "backend/testOnly jobby.tests.stub.*")
addCommandAlias("unitTests", "backend/testOnly jobby.tests.unit.*")
addCommandAlias(
  "fastTests",
  "backend/testOnly jobby.tests.stub.* jobby.tests.unit.*"
)
addCommandAlias(
  "integrationTests",
  "backend/testOnly jobby.tests.integration.*"
)
addCommandAlias(
  "frontendTests",
  "backend/testOnly jobby.tests.frontend.*"
)

addCommandAlias("lint", "scalafixAll; scalafmtAll")
addCommandAlias("lintCheck", "; scalafixAll --check ; scalafmtCheckAll")

ThisBuild / concurrentRestrictions ++= {
  if (sys.env.contains("CI")) {
    Seq(
      Tags.limitAll(4)
    )
  } else Seq.empty
}
