// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "Spreadsheets"

version := "0.1"

scalaVersion := "2.12.1"

libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
jsDependencies += "org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js"
