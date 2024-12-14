resolvers ++= Resolver.sonatypeOssRepos("public")

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "metaProject",
    // Compile-time dependencies
    libraryDependencies ++= Seq(
      "com.raquo" %% "buildkit" % "0.1.0",
    )
  )