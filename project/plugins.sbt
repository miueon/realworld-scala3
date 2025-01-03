addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % sys.env
    .getOrElse("SMITHY_VERSION", "0.18.23")
)
addSbtPlugin("io.spray"                    % "sbt-revolver"        % "0.9.1")
addSbtPlugin("com.github.sbt"              % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.eed3si9n"                % "sbt-projectmatrix"   % "0.9.0")
addSbtPlugin("org.scala-js"                % "sbt-scalajs"         % "1.16.0")
addSbtPlugin("com.timushev.sbt"            % "sbt-updates"         % "0.6.4")
addSbtPlugin("org.scalameta"               % "sbt-scalafmt"        % "2.5.0")
addSbtPlugin("com.armanbilge"              % "sbt-bundlemon"       % "0.1.3")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter"       % "1.0.0-beta44")
addSbtPlugin("nl.gn0s1s"                   % "sbt-dotenv"          % "3.0.0")
addSbtPlugin("ch.epfl.scala"               % "sbt-scalafix"        % "0.12.1")
addSbtPlugin("org.scalameta"               % "sbt-metals"          % "1.4.1")
addSbtPlugin("com.github.sbt"              % "sbt-dynver"          % "5.1.0")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
