import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.ModuleSplitStyle
import smithy4s.codegen.Smithy4sCodegenPlugin
import scala.sys.process.Process
import java.nio.file.*
import java.nio.charset.StandardCharsets
import com.raquo.buildkit.SourceDownloader

Compile / run / fork          := true
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalacOptions := Seq(
  "-Wunused:all",
  "-experimental",
  "-language:experimental.betterFors",
  "-Ycheck-all-patmat",
)
inThisBuild(
  Seq(
    semanticdbEnabled := true, // for scalafix
    semanticdbVersion := scalafixSemanticdb.revision,
    dynverSeparator   := "-"
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
  val scala             = "3.6.2"
  val jwt               = "9.1.2"
  val flyway            = "10.7.2"
  val postgres          = "42.6.0"
  val testContainers    = "0.41.4"
  val weaver            = "0.8.3"
  val weaverPlaywright  = "0.0.5"
  val laminar           = "15.0.1"
  val waypoint          = "6.0.0"
  val monocle           = "3.2.0"
  val circe             = "0.14.3"
  val macroTaskExecutor = "1.1.1"
  val cats              = "2.10.0"
  val password4j        = "1.7.3"
  val upickle           = "4.0.1"
  val ducktape          = "0.2.6"
  val laminext          = "0.15.0"
  val smithy4sFetch     = "0.0.4"
}

val Config = new {
  val DockerImageName = "miueon/realworld-smithy4s"
  val DockerBaseImage = "wonder/jdk:17.0.10_7-ubuntu"
  val BasePackage     = "realworld"
}

lazy val preload = taskKey[Unit]("runs Laminar-specific pre-load tasks")

preload := {
  val projectDir = (ThisBuild / baseDirectory).value

  SourceDownloader.downloadVersionedFile(
    name = "scalafmt-shared-conf",
    version = "v0.1.0",
    urlPattern =
      version => s"https://raw.githubusercontent.com/raquo/scalafmt-config/refs/tags/$version/.scalafmt.shared.conf",
    versionFile = projectDir / ".downloads" / ".scalafmt.shared.conf.version",
    outputFile = projectDir / ".downloads" / ".scalafmt.shared.conf",
    processOutput = "#\n# DO NOT EDIT. See SourceDownloader in build.sbt\n" + _
  )
}

Global / onLoad := {
  (Global / onLoad).value andThen { state => preload.key.label :: state }
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
  .jvmPlatform(Seq(Versions.scala))
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion              := Versions.scala,
    Compile / doc / sources   := Seq.empty,
    dockerBaseImage           := Config.DockerBaseImage,
    dockerUpdateLatest        := true,
    Docker / packageName      := Config.DockerImageName,
    Docker / dockerRepository := Some("ghcr.io"),
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % Versions.http4sBlaze,
      "org.http4s"    %% "http4s-ember-server" % Versions.http4s,
      "org.postgresql" % "postgresql"          % Versions.postgres,
      "ch.qos.logback" % "logback-classic"     % Versions.logback
    ),
    reStart / baseDirectory := (ThisBuild / baseDirectory).value,
    run / baseDirectory     := (ThisBuild / baseDirectory).value
  )

val iron = Seq(
  "io.github.iltotore" %% "iron"       % Versions.iron,
  "io.github.iltotore" %% "iron-cats"  % Versions.iron,
  "io.github.iltotore" %% "iron-circe" % Versions.iron,
  "io.github.iltotore" %% "iron-ciris" % Versions.iron
)

val db = Seq(
  "org.flywaydb"        % "flyway-database-postgresql" % Versions.flyway,
  "org.tpolecat"       %% "doobie-postgres"            % Versions.doobie,
  "org.tpolecat"       %% "doobie-hikari"              % Versions.doobie,
  "io.github.arturaz"  %% "doobie-typesafe"            % Versions.doobieTypeSafe,
  "io.github.iltotore" %% "iron-doobie"                % Versions.iron
)

lazy val backend = projectMatrix
  .in(file("modules/backend"))
  .dependsOn(shared)
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.scala))
  .settings(
    scalaVersion            := Versions.scala,
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
      "is.cir"                        %% "ciris-refined"           % Versions.ciris,
      "io.github.arainko"             %% "ducktape"                % Versions.ducktape
    ),
    libraryDependencies ++= db,
    libraryDependencies ++= iron,
    libraryDependencies ++=
      Seq(
        "org.typelevel"                %% "cats-core"                       % Versions.cats,
        "com.dimafeng"                 %% "testcontainers-scala-postgresql" % Versions.testContainers,
        "com.dimafeng"                 %% "testcontainers-scala-redis"      % Versions.testContainers,
        "com.indoorvivants.playwright" %% "weaver"                          % Versions.weaverPlaywright,
        "com.disneystreaming"          %% "weaver-cats"                     % Versions.weaver,
        "com.disneystreaming"          %% "weaver-scalacheck"               % Versions.weaver,
        "org.http4s"                   %% "http4s-blaze-server"             % Versions.http4sBlaze,
        "org.http4s"                   %% "http4s-blaze-client"             % Versions.http4sBlaze,
        "org.http4s"                   %% "http4s-ember-server"             % Versions.http4s,
        "org.http4s"                   %% "http4s-ember-client"             % Versions.http4s,
        "org.typelevel"                %% "log4cats-noop"                   % Versions.log4cats,
        "com.microsoft.playwright"      % "playwright"                      % "1.49.0"
      ).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Test / fork          := true,
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    Test / resourceGenerators += {
      Def.task[Seq[File]] {
        copyAll(buildFrontend.value, (Test / resourceManaged).value / "static")
      }
    }
  )

lazy val shared = projectMatrix
  .in(file("modules/shared"))
  .defaultAxes(defaults*)
  .jvmPlatform(Seq(Versions.scala))
  .jsPlatform(Seq(Versions.scala))
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
  .jsPlatform(Seq(Versions.scala))
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
      "com.raquo"          %%% "laminar"                     % Versions.laminar,
      "org.scala-js"       %%% "scala-js-macrotask-executor" % Versions.macroTaskExecutor,
      "io.laminext"        %%% "validation-cats"             % Versions.laminext,
      "tech.neander"       %%% "smithy4s-fetch"              % Versions.smithy4sFetch,
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
  Seq(VirtualAxis.scalaABIVersion(Versions.scala), VirtualAxis.jvm)

lazy val isRelease = sys.env.get("RELEASE").contains("yesh")

val buildFrontend = taskKey[File]("Build frontend")
ThisBuild / buildFrontend := {
  def frontendProj = frontend.finder(VirtualAxis.js)(Versions.scala)
  // val appDirPath      = app.base.getAbsolutePath()

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

  // IO.delete(app.base / "src" / "main" / "resources" / "static")
  // IO.copyDirectory(
  //   source = frontend.base / "dist",
  //   target = app.base / "src" / "main" / "resources" / "static"
  // )

  val log = streams.value.log
  def addStaticPathPrefix(file: Path): Unit = {
    val content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    val updatedContent = content.replaceAll(
      """(<script[^>]*\s+src=")([^"]+)""",
      s"""$$1/static$$2"""
    )
    Files.write(file, updatedContent.getBytes(StandardCharsets.UTF_8))
    log.info(s"Updated ${file.getFileName}")
  }

  val frontendDirPath = frontend.base.getAbsolutePath()
  addStaticPathPrefix(Paths.get(frontendDirPath, "dist", "index.html"))
  sbt.IO.toFile(Paths.get(frontendDirPath, "dist").toUri())
}

lazy val buildAndCopyFrontend = taskKey[Seq[File]]("Build and copy frontend")
buildAndCopyFrontend := {
  val frontendDir = buildFrontend.value
  copyAll(frontendDir, app.base / "src" / "main" / "resources" / "static")
}

def copyAll(location: File, outDir: File) = {
  IO.delete(outDir)
  IO.copyDirectory(location, outDir)
  def getAllFiles(dir: File): List[File] = {
    val these = dir.listFiles
    these.filter(_.isFile).toList ++ these.filter(_.isDirectory).flatMap(getAllFiles)
  }
  getAllFiles(outDir)
}

(app.finder(VirtualAxis.jvm)(Versions.scala) / Docker / publishLocal) := (app.finder(
  VirtualAxis.jvm
)(Versions.scala) / Docker / publishLocal).dependsOn(buildFrontend).value

addCommandAlias("publishDockerLocal", "app/Docker/publishLocal")
addCommandAlias("publishDocker", "app/Docker/publish")

ThisBuild / concurrentRestrictions ++= {
  if (sys.env.contains("CI")) {
    Seq(
      Tags.limitAll(4)
    )
  } else Seq.empty
}
