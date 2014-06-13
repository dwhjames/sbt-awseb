organization := "com.github.dwhjames"

name := "sbt-awseb"

version := "0.0.1-SNAPSHOT"

description := "SBT plugin for Amazon Web Services Elastic Beanstalk"

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

homepage := Some(url("https://github/dwhjames/sbt-awseb"))

sbtPlugin := true

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.7.12"
