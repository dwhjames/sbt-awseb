package awseb

import sbt._
import Keys._

import scala.collection.JavaConverters._

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._

object AWSEBPlugin extends sbt.AutoPlugin {
  object autoImport {
    type Region = com.amazonaws.regions.Region

    val awsCredentialsProfileName = SettingKey[Option[String]](
      "awsCredentialsProfileName", "The name of the AWS credentials profile")

    val awsCredentialsProvider = SettingKey[AWSCredentialsProvider](
      "awsCredentialsProvider", "The provider of AWS credentials")

    val awseb = TaskKey[String](
      "awseb", "sbt-awseb is an interface for AWS Elastic Beanstalk")

    val ebAppName = SettingKey[String](
      "ebAppName", "The name of the Beanstalk application, defaults to the module name")

    val ebEventLimit = SettingKey[Int](
      "ebEventLimit", "The limit for the number of recent events to list")

    val ebRegion = SettingKey[Region](
      "ebRegion", "The AWS region in which to communicate with Elastic Beanstalk")

  }
  import autoImport._
  // override def trigger = allRequirements

  val ebClient = SettingKey[AWSElasticBeanstalkClient](
    "ebClient", "An AWS Elastic Beanstalk client")

  val checkDNSAvailability = InputKey[Unit](
    "checkDNSAvailability", "Check if the specified CNAME is available")

  val deleteApplication = TaskKey[Unit](
    "deleteApplication", "Delete the application")

  val deleteApplicationVersion = InputKey[Unit](
    "deleteApplicationVersion", "Delete the specified application versions")

  val describeApplication = TaskKey[Unit](
    "describeApplication", "Describe the application")

  val describeApplicationVersions = TaskKey[Unit](
    "describeApplicationVersions", "Describe the version of the application")

  val describeEnvironments = TaskKey[Unit](
    "describeEnvironments", "Describe the environments")

  val describeEnvironmentResources = InputKey[Unit](
    "describeEnvironmentResources", "Describe the resources of the specified environment")

  val describeEvents = InputKey[Unit](
    "describeEvents", "Describe the recent events for an application")

  val listAvailableSolutionStacks = TaskKey[Unit](
    "listAvailableSolutionStacks", "List the available solution stacks")

  val rebuildEnvironment = InputKey[Unit](
    "rebuildEnvironment", "Rebuild the specified environments")

  val requestEnvironmentInfo = InputKey[Unit](
    "requestEnvironmentInfo", "Request information for the specified environments")

  val restartAppServer = InputKey[Unit](
    "restartAppServer", "Restart the app server for the specified environments")

  val retrieveEnvironmentInfo = InputKey[Unit](
    "retrieveEnvironmentInfo", "Retrieve information for the specified environments")


  val awsCredentialsProviderSetting: Def.Initialize[AWSCredentialsProvider] =
    Def.setting {
      awsCredentialsProfileName.value match {
        case None =>
          new ProfileCredentialsProvider()
        case Some(profileName) =>
          new ProfileCredentialsProvider(profileName)
      }
    }


  val ebClientSetting: Def.Initialize[AWSElasticBeanstalkClient] =
    Def.setting {
      val client = new AWSElasticBeanstalkClient(awsCredentialsProvider.value)
      client.setRegion((ebRegion in awseb).value)
      client
    }


  private def checkDNSAvailabilityTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<CNAME>").parsed
      if (args.length > 0) {
        val cNAMEPrefix = args(0)
        val res = (ebClient in awseb).value.checkDNSAvailability(new CheckDNSAvailabilityRequest(cNAMEPrefix))
        val log = streams.value.log
        log.info(s"""${Option(res.getFullyQualifiedCNAME).getOrElse(cNAMEPrefix)} is ${if (res.isAvailable) "" else "not"} available""")
      }
    }


  private def deleteApplicationTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val applicationName = (ebAppName in awseb).value
      val log = streams.value.log
      log.info(s"Requesting the deletion of application $applicationName")
      (ebClient in awseb).value.deleteApplication(new DeleteApplicationRequest(applicationName))
    }


  private def deleteApplicationVersionTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<version label>").parsed
      val applicationName = (ebAppName in awseb).value
      val log = streams.value.log
      val client = (ebClient in awseb).value
      val req = new DeleteApplicationVersionRequest().
                  withApplicationName(applicationName).
                  withDeleteSourceBundle(true)

      args foreach { verLabel =>
        log.info(s"Requesting the deletion of application version $verLabel")
        client.deleteApplicationVersion(req.withVersionLabel(verLabel))
      }
    }


  private def describeApplicationTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val applicationName = (ebAppName in awseb).value
      val log = streams.value.log
      val applications = (ebClient in awseb).value.describeApplications(
          new DescribeApplicationsRequest()
            .withApplicationNames(applicationName)
        ).getApplications.asScala

      if (applications.isEmpty) {
        log.warn(s"No application called ${applicationName} was found!")
      }

      applications foreach { app =>
        log.info(s"""
        | Application name: ${app.getApplicationName}
        | Configuration templates: ${app.getConfigurationTemplates.asScala.mkString(", ")}
        | Date created: ${app.getDateCreated}
        | Date updated: ${app.getDateUpdated}
        | Description: ${app.getDescription}
        | Versions: ${app.getVersions.asScala.mkString(", ")}
        | ------
        |""".stripMargin)
      }
    }


  private def describeApplicationVersionsTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val applicationName = (ebAppName in awseb).value
      val log = streams.value.log
      val versions = (ebClient in awseb).value.describeApplicationVersions(
          new DescribeApplicationVersionsRequest()
            .withApplicationName(applicationName)
        ).getApplicationVersions.asScala

      if (versions.isEmpty) {
        log.warn(s"No versions for application ${applicationName} were found!")
      }

      versions foreach { ver =>
        val sourceBundle = ver.getSourceBundle
        log.info(s"""
        | Application name: ${ver.getApplicationName}
        | Date created: ${ver.getDateCreated}
        | Date updated: ${ver.getDateUpdated}
        | Description: ${ver.getDescription}
        | Source bundle: ${ver.getSourceBundle}
        | Version label: ${ver.getVersionLabel}
        | ------
        |""".stripMargin)
      }
    }


  private def describeEnvironmentsTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val applicationName = (ebAppName in awseb).value
      val log = streams.value.log
      val environments = (ebClient in awseb).value.describeEnvironments(
          new DescribeEnvironmentsRequest().withApplicationName(applicationName)
        ).getEnvironments.asScala

      if (environments.isEmpty) {
        log.warn(s"No environments for application ${applicationName} were found!")
      }

      environments foreach { env =>
        log.info(s"""
        | Application name: ${env.getApplicationName}
        | CNAME: ${env.getCNAME}
        | Date created: ${env.getDateCreated}
        | Date updated: ${env.getDateUpdated}
        | Description: ${env.getDescription}
        | Endpoint URL: ${env.getEndpointURL}
        | Environment Id: ${env.getEnvironmentId}
        | Environment name: ${env.getEnvironmentName}
        | Health: ${env.getHealth}
        | Solution stack name: ${env.getSolutionStackName}
        | Status: ${env.getStatus}
        | Template name: ${env.getTemplateName}
        | Version label: ${env.getVersionLabel}
        | Deployed version: ${env.getVersionLabel}
        | ------
        |""".stripMargin)
      }
    }


  private def describeEnvironmentResourcesTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val log = streams.value.log
      if (args.length != 1) {
        log.error("describeEnvironmentResources requires an environment name")
      } else {
        val resources = (ebClient in awseb).value.describeEnvironmentResources(
            new DescribeEnvironmentResourcesRequest()
              .withEnvironmentName(args(0))
          ).getEnvironmentResources()

        log.info(s"""
        | Environment name: ${resources.getEnvironmentName}
        | Autoscaling groups: ${resources.getAutoScalingGroups.asScala.map(_.getName).mkString(", ")}
        | Instances: ${resources.getInstances.asScala.map(_.getId).mkString(", ")}
        | Launch configurations: ${resources.getLaunchConfigurations.asScala.map(_.getName).mkString(", ")}
        | Load balancers: ${resources.getLoadBalancers.asScala.map(_.getName).mkString(", ")}
        | Queues: ${resources.getQueues.asScala.mkString(", ")}
        | Triggers: ${resources.getTriggers.asScala.map(_.getName).mkString(", ")}
        | ------
        |""".stripMargin)
      }
    }


  private def describeEventsTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val applicationName = (ebAppName in awseb).value
      val limit = (ebEventLimit in awseb).value
      val log = streams.value.log
      val req = new DescribeEventsRequest().
                  withApplicationName(applicationName).
                  withMaxRecords(limit)

      if (args.length > 0) {
        req.setEnvironmentId(args(0))
      }

      val events = (ebClient in awseb).value.describeEvents(req).getEvents().asScala

      events foreach { ev =>
        log.info(s"""
        | Application name: ${ev.getApplicationName}
        | Environment name: ${ev.getEnvironmentName}
        | Event date: ${ev.getEventDate}
        | Message: ${ev.getMessage}
        | Request Id: ${ev.getRequestId}
        | Severity: ${ev.getSeverity}
        | Template name: ${ev.getTemplateName}
        | Version label: ${ev.getVersionLabel}
        | ------
        |""".stripMargin)
      }
    }


  private def listAvailableSolutionStacksTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log
      (ebClient in awseb).value.listAvailableSolutionStacks().getSolutionStackDetails.asScala foreach { solStack =>
        log.info(s"""
        | Solution stack name: ${solStack.getSolutionStackName}
        | Permitted file types: ${solStack.getPermittedFileTypes.asScala.mkString(", ")}
        | ------
        |""".stripMargin)
      }
    }


  private def rebuildEnvironmentTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val log = streams.value.log
      val client = (ebClient in awseb).value

      args foreach { envName =>
        log.info(s"Requesting that environment $envName be rebuilt")
        client.rebuildEnvironment(new RebuildEnvironmentRequest().withEnvironmentName(envName))
      }
    }


  private def requestEnvironmentInfoTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val log = streams.value.log
      val client = (ebClient in awseb).value
      val req = new RequestEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)

      args foreach { envName =>
        log.info(s"Requesting information for environment $envName")
        client.requestEnvironmentInfo(req.withEnvironmentName(envName))
      }
    }


  private def restartAppServerTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val log = streams.value.log
      val client = (ebClient in awseb).value

      args foreach { envName =>
        log.info(s"Requesting that app server for environment $envName be restarted")
        client.restartAppServer(new RestartAppServerRequest().withEnvironmentName(envName))
      }
    }


  private def retrieveEnvironmentInfoTask: Def.Initialize[InputTask[Unit]] =
    Def.inputTask {
      val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
      val log = streams.value.log
      val client = (ebClient in awseb).value
      val req = new RetrieveEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)

      args foreach { envName =>
        log.info(s"Retrieving information for environment $envName")
        client.retrieveEnvironmentInfo(req.withEnvironmentName(envName)).getEnvironmentInfo.asScala foreach { envInfo =>
          log.info(s"""
          | EC2 instance Id: ${envInfo.getEc2InstanceId}
          | Info type: ${envInfo.getInfoType}
          | Message: ${envInfo.getMessage}
          | Sample timestamp: ${envInfo.getSampleTimestamp}
          | ------
          |""".stripMargin)
        }
      }
    }

  override lazy val buildSettings =
    Seq(
      awsCredentialsProfileName := None,
      awsCredentialsProvider := awsCredentialsProviderSetting.value,
      ebClient in awseb := ebClientSetting.value,
      ebEventLimit in awseb := 10,
      ebRegion in awseb := Region.getRegion(Regions.US_EAST_1),
      checkDNSAvailability in awseb <<= checkDNSAvailabilityTask,
      listAvailableSolutionStacks in awseb <<= listAvailableSolutionStacksTask
    )

  override lazy val projectSettings =
    Seq(
      ebAppName in awseb := moduleName.value,
      deleteApplication in awseb <<= deleteApplicationTask,
      deleteApplicationVersion in awseb <<= deleteApplicationVersionTask,
      describeApplication in awseb <<= describeApplicationTask,
      describeApplicationVersions in awseb <<= describeApplicationVersionsTask,
      describeEnvironments in awseb <<= describeEnvironmentsTask,
      describeEnvironmentResources in awseb <<= describeEnvironmentResourcesTask,
      describeEvents in awseb <<= describeEventsTask,
      rebuildEnvironment in awseb <<= rebuildEnvironmentTask,
      requestEnvironmentInfo in awseb <<= requestEnvironmentInfoTask,
      restartAppServer in awseb <<= restartAppServerTask,
      retrieveEnvironmentInfo in awseb <<= retrieveEnvironmentInfoTask
    )
}
