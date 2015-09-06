lazy val commonSettings = Seq(
  organization := "edu.illinois.i3.htrc.apps.idfinder",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-feature", "-language:postfixOps", "-target:jvm-1.8"),
  resolvers ++= Seq(
    "I3 Repository" at "http://htrc.illinois.edu/nexus/content/groups/public"
  )
)

lazy val idfinder = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  settings(commonSettings: _*).
  settings(
    name := "idfinder",
    version := "1.1",
    libraryDependencies ++= Seq(
      "org.rogach"                    %% "scallop"            % "0.9.5",
      "ch.qos.logback"                %  "logback-classic"    % "1.1.3",
      "com.typesafe.scala-logging"    %% "scala-logging"      % "3.1.0",
      "com.github.tototoshi"          %% "scala-csv"          % "1.2.2",
      "com.jsuereth"                  %% "scala-arm"          % "1.4",
      "net.databinder.dispatch"       %% "dispatch-core"      % "0.11.3",
      "net.databinder.dispatch"       %% "dispatch-json4s-native" % "0.11.3",
      "org.scalatest"                 %% "scalatest"          % "2.2.1"        % Test
    )
  )
