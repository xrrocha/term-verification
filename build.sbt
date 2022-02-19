name := "term-verification"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-deprecation", "-language:experimental.macros")

resolvers ++=  Seq(
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "sbt-plugin-releases" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "org.scalatra" %% "scalatra" % "2.6.5",
  "org.scalatra" %% "scalatra-json" % "2.6.5",
  "org.json4s"   %% "json4s-jackson" % "3.6.6",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "org.postgresql" % "postgresql" % "42.2.5" % "runtime",
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "com.h2database" % "h2" % "1.4.199" % Test,
  "org.hsqldb" % "hsqldb" % "2.5.0" % Test,
  "org.apache.derby" % "derby" % "10.9.1.0" % Test,
  "org.apache.commons" % "commons-dbcp2" % "2.6.0" % Test,
  "org.scalatra" %% "scalatra-scalatest" % "2.6.5" % Test cross CrossVersion.binary,
  "commons-beanutils" % "commons-beanutils" % "1.9.3",
  "commons-logging" % "commons-logging" % "1.2" % "runtime"
)

//seq(coffeeSettings :_*)
//
//// source-directory: src/main/coffee
//// resource-managed: target/:scala-version/resource_managed/main/js
//sourceDirectory in (Compile, CoffeeKeys.coffee) <<= sourceDirectory { _ / "main" / "coffee" }
//
//(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (crossTarget in Compile)(_ / ".." / ".." / "src" / "main" / "webapp" / "js")

enablePlugins(JettyPlugin)

// seq(webSettings :_*)
// port in container.Configuration := 9090
// 
// libraryDependencies ++= Seq(
//   "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container"
// )

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    artifact.name + "." + artifact.extension
}
