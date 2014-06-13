import bintray.Keys._

sbtPlugin := true


organization := "com.github.dwhjames"

name := "sbt-awseb"

version := "0.1.0"

description := "SBT plugin for Amazon Web Services Elastic Beanstalk"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

homepage := Some(url("https://github/dwhjames/sbt-awseb"))


libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.7.12"


publishMavenStyle := false

publishArtifact in (Compile, packageDoc) := false


bintrayPublishSettings

repository in bintray := "sbt-plugins"

bintrayOrganization in bintray := None

packageLabels in bintray := Seq("sbt", "sbt-plugin", "aws", "elasticbeanstalk")
