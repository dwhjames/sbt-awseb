package awseb

import sbt._
import Keys._

import scala.collection.JavaConverters._

import java.io.File

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3.AmazonS3Client


object AWSEBPlugin extends sbt.AutoPlugin {
  object autoImport {
    case class EBEnvironment(envName: String, cname: String, solutionStackName: String)

    val awsCredentialsProfileName = Def.settingKey[Option[String]]("The name of the AWS credentials profile")

    val awsCredentialsProvider = Def.settingKey[AWSCredentialsProvider]("The provider of AWS credentials")

    val awseb = Def.taskKey[String]("sbt-awseb is an interface for AWS Elastic Beanstalk")

    val ebAppName = Def.settingKey[String]("The name of the Beanstalk application, defaults to the module name")

    val ebAppBundle = Def.taskKey[File]("The bundle file that packages the Beanstalk application")

    val ebAppDescription = Def.settingKey[Option[String]]("The description of the Beanstalk application, defaults to None")

    val ebAppVersionLabel = Def.taskKey[String]("A version label generator")

    val ebEnvMap = Def.settingKey[Map[String, EBEnvironment]]("The map of environments for a Beanstalk application, defaults to empty")

    val ebEventLimit = Def.settingKey[Int]("The limit for the number of recent events to list")

    val ebRegion = Def.settingKey[Region]("The AWS region in which to communicate with Elastic Beanstalk")

    val s3AppBucketName = Def.settingKey[String]("The S3 bucket in which to store app bundles")

  }
  import autoImport._
  // override def trigger = allRequirements

  val ebClient = Def.settingKey[AWSElasticBeanstalkClient]("An AWS Elastic Beanstalk client")

  val s3Client = Def.settingKey[AmazonS3Client]("An AWS S3 client")

  val checkDNSAvailability = Def.inputKey[Unit]("Check if the specified CNAME is available")

  val cleanApplicationVersions = Def.taskKey[Unit]("Remove the unused application versions")

  val createAppBucket = Def.taskKey[Unit]("Create an S3 bucket in which to store app bundles")

  val createApplication = Def.taskKey[Unit]("Create an Elastic Beanstalk application")

  val createApplicationVersion = Def.inputKey[String]("Create an Elastic Beanstalk application, returning the version label")

  val deleteAppBucket = Def.taskKey[Unit]("Delete the S3 bucket that stores app bundles")

  val deleteApplication = Def.taskKey[Unit]("Delete the application")

  val deleteApplicationVersion = Def.inputKey[Unit]("Delete the specified application versions")

  val describeApplication = Def.taskKey[Unit]("Describe the application")

  val describeApplicationVersions = Def.taskKey[Unit]("Describe the version of the application")

  val describeEnvironments = Def.taskKey[Unit]("Describe the environments")

  val describeEnvironmentResources = Def.inputKey[Unit]("Describe the resources of the specified environment")

  val describeEvents = Def.inputKey[Unit]("Describe the recent events for an application")

  val listAvailableSolutionStacks = Def.taskKey[Unit]("List the available solution stacks")

  val rebuildEnvironment = Def.inputKey[Unit]("Rebuild the specified environments")

  val requestEnvironmentInfo = Def.inputKey[Unit]("Request information for the specified environments")

  val restartAppServer = Def.inputKey[Unit]("Restart the app server for the specified environments")

  val retrieveEnvironmentInfo = Def.inputKey[Unit]("Retrieve information for the specified environments")

  val swapEnvironmentCNAMEs = Def.inputKey[Unit]("Swap the CNAMEs of the two specified environments")

  val terminateEnvironment = Def.inputKey[Unit]("Terminate the specified environments")

  val updateApplication = Def.taskKey[Unit]("Update the application description")

  val uploadAppBundle = Def.taskKey[S3Location]("Upload the application bundle to S3")


  val awsCredentialsProviderSetting = Def.setting[AWSCredentialsProvider] {
    awsCredentialsProfileName.value match {
      case None =>
        new ProfileCredentialsProvider()
      case Some(profileName) =>
        new ProfileCredentialsProvider(profileName)
    }
  }


  val ebClientSetting = Def.setting[AWSElasticBeanstalkClient] {
    val client = new AWSElasticBeanstalkClient(awsCredentialsProvider.value)
    client.setRegion((ebRegion in awseb).value)
    client
  }


  val s3ClientSetting = Def.setting[AmazonS3Client] {
    val client = new AmazonS3Client(awsCredentialsProvider.value)
    client.setRegion((ebRegion in awseb).value)
    client
  }


  private def checkDNSAvailabilityTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<CNAME>").parsed
    if (args.length > 0) {
      val cNAMEPrefix = args(0)
      val res = (ebClient in awseb).value.checkDNSAvailability(new CheckDNSAvailabilityRequest(cNAMEPrefix))
      val log = streams.value.log
      log.info(s"""${Option(res.getFullyQualifiedCNAME).getOrElse(cNAMEPrefix)} is ${if (res.isAvailable) "" else "not"} available""")
    }
  }


  private def cleanApplicationVersionsTask = Def.task[Unit] {
    val applicationName = (ebAppName in awseb).value
    val log = streams.value.log
    val client = (ebClient in awseb).value

    val applicationVersions = {
      val apps = client.describeApplications(
          new DescribeApplicationsRequest()
            .withApplicationNames(applicationName)
        ).getApplications.asScala
      if (apps.isEmpty) {
        val msg = s"No application called ${applicationName} was found!"
        log.error(msg)
        throw new IllegalStateException(msg)
      } else apps.head.getVersions.asScala.toSet
    }
    log.debug(s"Versions for application $applicationName: $applicationVersions")

    val deployedApplicationVersions = client.describeEnvironments(
        new DescribeEnvironmentsRequest().withApplicationName(applicationName)
      ).getEnvironments.asScala.map(_.getVersionLabel).toSet
    log.debug(s"Deployed versions for application $applicationName: $deployedApplicationVersions")

    val deleteReq = new DeleteApplicationVersionRequest().
                      withApplicationName(applicationName).
                      withDeleteSourceBundle(true)

    (applicationVersions diff deployedApplicationVersions) foreach { verLabel =>
      log.info(s"Requesting the deletion of application version $verLabel")
      client.deleteApplicationVersion(deleteReq.withVersionLabel(verLabel))
    }
  }


  private def createAppBucketTask = Def.task[Unit] {
    val client = (s3Client in awseb).value
    val bucketName = (s3AppBucketName in awseb).value
    val bucket = client.createBucket(bucketName)

    streams.value.log.info(s"""
    | Creation Date: ${bucket.getCreationDate}
    | Name: ${bucket.getName}
    | Owner: ${bucket.getOwner}
    | ------
    |""".stripMargin)
  }


  private def applicationDescriptionToString(app: ApplicationDescription): String = {
    s"""
    | Application name: ${app.getApplicationName}
    | Configuration templates: ${app.getConfigurationTemplates.asScala.mkString(", ")}
    | Date created: ${app.getDateCreated}
    | Date updated: ${app.getDateUpdated}
    | Description: ${app.getDescription}
    | Versions: ${app.getVersions.asScala.mkString(", ")}
    | ------
    |""".stripMargin
  }


  private def createApplicationTask = Def.task[Unit] {
    val client = (ebClient in awseb).value
    val applicationName = (ebAppName in awseb).value
    val req = new CreateApplicationRequest(applicationName)
    val applicationDescription = (ebAppDescription in awseb).value
    applicationDescription foreach { desc => req.setDescription(desc) }

    val app = client.createApplication(req).getApplication()

    streams.value.log.info(applicationDescriptionToString(app))
  }


  private def applicationVersionDescriptionToString(ver: ApplicationVersionDescription): String = {
    s"""
    | Application name: ${ver.getApplicationName}
    | Date created: ${ver.getDateCreated}
    | Date updated: ${ver.getDateUpdated}
    | Description: ${ver.getDescription}
    | Source bundle: ${ver.getSourceBundle}
    | Version label: ${ver.getVersionLabel}
    | ------
    |""".stripMargin
  }


  private def createApplicationVersionTask = Def.inputTask[String] {
    val args: Seq[String] = Def.spaceDelimited("<version description>").parsed
    val versionDescription = args.mkString(", ")
    val log = streams.value.log
    val applicationName = (ebAppName in awseb).value
    val versionLabel = (ebAppVersionLabel in awseb).value

    log.info(s"Creating version $versionLabel for application $applicationName")
    val app = (ebClient in awseb).value.createApplicationVersion(
        new CreateApplicationVersionRequest(applicationName, versionLabel)
          .withDescription(versionDescription)
          .withSourceBundle((uploadAppBundle in awseb).value)
      ).getApplicationVersion()

    log.info(applicationVersionDescriptionToString(app))
    versionLabel
  }


  private def deleteAppBucketTask = Def.task[Unit] {
    val client = (s3Client in awseb).value
    val bucketName = (s3AppBucketName in awseb).value
    streams.value.log.info(s"Requesting that S3 bucket $bucketName be deleted")
    client.deleteBucket(bucketName)
  }


  private def deleteApplicationTask = Def.task[Unit] {
    val applicationName = (ebAppName in awseb).value
    val log = streams.value.log
    log.info(s"Requesting the deletion of application $applicationName")
    (ebClient in awseb).value.deleteApplication(new DeleteApplicationRequest(applicationName))
  }


  private def deleteApplicationVersionTask = Def.inputTask[Unit] {
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


  private def describeApplicationTask = Def.task[Unit] {
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
      log.info(applicationDescriptionToString(app))
    }
  }


  private def describeApplicationVersionsTask = Def.task[Unit] {
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
      log.info(applicationVersionDescriptionToString(ver))
    }
  }


  private def describeEnvironmentsTask = Def.task[Unit] {
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


  private def describeEnvironmentResourcesTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
      val resources = (ebClient in awseb).value.describeEnvironmentResources(
          new DescribeEnvironmentResourcesRequest()
            .withEnvironmentName(envName)
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


  private def describeEventsTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val applicationName = (ebAppName in awseb).value
    val limit = (ebEventLimit in awseb).value
    val log = streams.value.log
    val req = new DescribeEventsRequest().
                withApplicationName(applicationName).
                withMaxRecords(limit)

    if (args.length > 0) {
      val envMap = (ebEnvMap in awseb).value
      val envName = envMap.get(args(0)).map(_.envName).getOrElse(args(0))
      req.setEnvironmentName(envName)
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


  private def listAvailableSolutionStacksTask = Def.task[Unit] {
    val log = streams.value.log
    (ebClient in awseb).value.listAvailableSolutionStacks().getSolutionStackDetails.asScala foreach { solStack =>
      log.info(s"""
      | Solution stack name: ${solStack.getSolutionStackName}
      | Permitted file types: ${solStack.getPermittedFileTypes.asScala.mkString(", ")}
      | ------
      |""".stripMargin)
    }
  }


  private def rebuildEnvironmentTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
      log.info(s"Requesting that environment $envName be rebuilt")
      client.rebuildEnvironment(new RebuildEnvironmentRequest().withEnvironmentName(envName))
    }
  }


  private def requestEnvironmentInfoTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new RequestEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
      log.info(s"Requesting information for environment $envName")
      client.requestEnvironmentInfo(req.withEnvironmentName(envName))
    }
  }


  private def restartAppServerTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
      log.info(s"Requesting that app server for environment $envName be restarted")
      client.restartAppServer(new RestartAppServerRequest().withEnvironmentName(envName))
    }
  }


  private def retrieveEnvironmentInfoTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new RetrieveEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
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


  private def swapEnvironmentCNAMEsTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value

    if (args.length != 2) {
      log.error(s"swapEnvironmentCNAMEs requires two environment names")
    } else {
      val envName1 = envMap.get(args(0)).map(_.envName).getOrElse(args(0))
      val envName2 = envMap.get(args(1)).map(_.envName).getOrElse(args(1))
      log.info(s"Requesting a swap of the CNAME from environments ${envName1} to ${envName2}")
      (ebClient in awseb).value.swapEnvironmentCNAMEs(
          new SwapEnvironmentCNAMEsRequest()
            .withSourceEnvironmentName(envName1)
            .withDestinationEnvironmentName(envName2)
        )
    }
  }


  private def terminateEnvironmentTask = Def.inputTask[Unit] {
    val args: Seq[String] = Def.spaceDelimited("<environment name>").parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new TerminateEnvironmentRequest().withTerminateResources(true)
    val envMap = (ebEnvMap in awseb).value

    args foreach { arg =>
      val envName = envMap.get(arg).map(_.envName).getOrElse(arg)
      log.info(s"Requesting termination of environment $envName")
      val res = client.terminateEnvironment(req.withEnvironmentName(envName))
      log.info(s"""
      | Application name: ${res.getApplicationName}
      | CNAME: ${res.getCNAME}
      | Date created: ${res.getDateCreated}
      | Date updated: ${res.getDateUpdated}
      | Description: ${res.getDescription}
      | Endpoint URL: ${res.getEndpointURL}
      | Environment Id: ${res.getEnvironmentId}
      | Environment name: ${res.getEnvironmentName}
      | Health: ${res.getHealth}
      | Solution stack name: ${res.getSolutionStackName}
      | Status: ${res.getStatus}
      | Template name: ${res.getTemplateName}
      | Tier: ${res.getTier}
      | Version label: ${res.getVersionLabel}
      | ------
      |""".stripMargin)
    }
  }


  private def updateApplicationTask = Def.task[Unit] {
    val client = (ebClient in awseb).value
    val applicationName = (ebAppName in awseb).value

    val app = client.updateApplication(
                new UpdateApplicationRequest(applicationName)
                  // empty string will zero out description on Beanstalk
                  .withDescription((ebAppDescription in awseb).value.getOrElse(""))
              ).getApplication()

    streams.value.log.info(applicationDescriptionToString(app))
  }


  private def uploadAppBundleTask = Def.task[S3Location] {
    val log = streams.value.log
    val client = (s3Client in awseb).value
    val bucketName = (s3AppBucketName in awseb).value
    val file = (ebAppBundle in awseb).value

    log.info(s"Uploading application bundle from $file to bucket $bucketName")
    val location = S3FileUploader.upload(client, bucketName, file)
    log.info(s"$location")
    location
  }


  override lazy val buildSettings =
    Seq(
      awsCredentialsProfileName := None,
      awsCredentialsProvider := awsCredentialsProviderSetting.value,
      ebClient in awseb := ebClientSetting.value,
      ebEventLimit in awseb := 10,
      ebRegion in awseb := Region.getRegion(Regions.US_EAST_1),
      s3Client in awseb := s3ClientSetting.value,
      checkDNSAvailability in awseb <<= checkDNSAvailabilityTask,
      listAvailableSolutionStacks in awseb <<= listAvailableSolutionStacksTask
    )

  override lazy val projectSettings =
    Seq(
      ebAppName in awseb := moduleName.value,
      ebAppDescription in awseb := None,
      s3AppBucketName in awseb <<= Def.setting[String] { "sbt-awseb-bundle-" + (ebAppName in awseb).value },
      ebAppVersionLabel in awseb <<= Def.task[String] { s"${version.value}-${ISO8601.timestamp()}" },
      ebEnvMap in awseb := Map.empty[String, EBEnvironment],
      cleanApplicationVersions in awseb <<= cleanApplicationVersionsTask,
      createAppBucket in awseb <<= createAppBucketTask,
      createApplication in awseb <<= createApplicationTask,
      createApplicationVersion in awseb <<= createApplicationVersionTask,
      deleteAppBucket in awseb <<= deleteAppBucketTask,
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
      retrieveEnvironmentInfo in awseb <<= retrieveEnvironmentInfoTask,
      swapEnvironmentCNAMEs in awseb <<= swapEnvironmentCNAMEsTask,
      terminateEnvironment in awseb <<= terminateEnvironmentTask,
      updateApplication in awseb <<= updateApplicationTask,
      uploadAppBundle in awseb <<= uploadAppBundleTask
    )
}
