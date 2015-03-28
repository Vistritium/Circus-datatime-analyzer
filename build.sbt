import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := """circus-datatime-analyzer"""

organization  := "com.maciejnowicki"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "repo typesafe" at """http://repo.typesafe.com/typesafe/releases/"""

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
    "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "io.spray" %%  "spray-json" % "1.3.1"
  )
}

Revolver.settings
