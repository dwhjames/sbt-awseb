package awseb

import sbt.complete.Parser
import sbt.complete.DefaultParsers._

object CustomParsers {

  lazy val CNAME: Parser[String] =
    Space ~> token(StringBasic, "<CNAME>")

  lazy val EnvironmentAlias: Parser[String] =
    Space ~> token(StringBasic, "<environment alias>")

  lazy val EnvironmentAliasOrName: Parser[String] =
    Space ~> token(StringBasic, "<environment alias or name>")

  lazy val EnvironmentAliasOrNamePair: Parser[(String, String)] =
    EnvironmentAliasOrName ~ EnvironmentAliasOrName

  lazy val EnvironmentAliasAndVersionLabel: Parser[(String, String)] =
    EnvironmentAlias ~ VersionLabel

  lazy val EnvironmentAliasAndOptionalVersionLabel: Parser[(String, Option[String])] =
    EnvironmentAlias ~ VersionLabel.?

  lazy val VersionDescription: Parser[String] =
    Space ~> token(StringBasic, "<version description>")

  lazy val VersionLabel: Parser[String] =
    Space ~> token(StringBasic, "<version label>")

  lazy val VersionLabelAndDescription: Parser[(String, String)] =
    VersionLabel ~ VersionDescription
}
