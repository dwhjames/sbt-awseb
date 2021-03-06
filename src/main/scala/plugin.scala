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

  val createApplicationVersion = Def.inputKey[String]("Create an Elastic Beanstalk application version, returning the version label")

  val createEnvironment = Def.inputKey[String]("Create an Elastic Beanstalk environment")

  val deleteAppBucket = Def.taskKey[Unit]("Delete the S3 bucket that stores app bundles")

  val deleteApplication = Def.taskKey[Unit]("Delete the application")

  val deleteApplicationVersion = Def.inputKey[Unit]("Delete the specified application version")

  val describeApplication = Def.taskKey[Unit]("Describe the application")

  val describeApplicationVersions = Def.taskKey[Unit]("Describe the version of the application")

  val describeConfigurationOptions = Def.inputKey[Unit]("Describe the configuration options for the specified environment")

  val describeConfigurationSettings = Def.inputKey[Unit]("Describe the configuration options for the specified environment")

  val describeEnvironments = Def.taskKey[Unit]("Describe the environments")

  val describeEnvironmentResources = Def.inputKey[Unit]("Describe the resources of the specified environment")

  val describeEvents = Def.inputKey[Unit]("Describe the recent events for an application")

  val listAvailableSolutionStacks = Def.taskKey[Unit]("List the available solution stacks")

  val rebuildEnvironment = Def.inputKey[Unit]("Rebuild the specified environment")

  val requestEnvironmentInfo = Def.inputKey[Unit]("Request information for the specified environment")

  val restartAppServer = Def.inputKey[Unit]("Restart the app server for the specified environment")

  val retrieveEnvironmentInfo = Def.inputKey[Unit]("Retrieve information for the specified environment")

  val swapEnvironmentCNAMEs = Def.inputKey[Unit]("Swap the CNAMEs of the two specified environments")

  val terminateEnvironment = Def.inputKey[Unit]("Terminate the specified environment")

  val updateApplication = Def.taskKey[Unit]("Update the application description")

  val updateApplicationVersion = Def.inputKey[Unit]("Update the description for the specified application version")

  val uploadAppBundle = Def.taskKey[S3Location]("Upload the application bundle to S3")

  val updateEnvironmentVersion = Def.inputKey[String]("Update the specified environment to the specified version, returning the version label")


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
    val cNAMEPrefix: String = CustomParsers.CNAME.parsed
    val res = (ebClient in awseb).value.checkDNSAvailability(new CheckDNSAvailabilityRequest(cNAMEPrefix))
    streams.value.log.info(s"""${Option(res.getFullyQualifiedCNAME).getOrElse(cNAMEPrefix)} is ${if (res.isAvailable) "" else "not "}available""")
  }


  private def cleanApplicationVersionsTask = Def.task[Unit] {
    val applicationName = (ebAppName in awseb).value
    val log = streams.value.log
    val client = (ebClient in awseb).value

    val applicationVersions =
      client.describeApplications(
        new DescribeApplicationsRequest().withApplicationNames(applicationName)
      ).getApplications.asScala.headOption match {
        case None =>
          val msg = s"No application called ${applicationName} was found!"
          log.error(msg)
          throw new IllegalStateException(msg)
        case Some(app) =>
          app.getVersions.asScala.toSet
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
    val versionDescription = CustomParsers.VersionDescription.parsed
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


  private def createEnvironmentTask = Def.inputTaskDyn[String] {
    val (alias, optLabel) = CustomParsers.EnvironmentAliasAndOptionalVersionLabel.parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value

    envMap.get(alias) match {
      case None =>
        val msg = "createEnvironmentTask requires an environment alias defined in ebEnvMap"
        log.error(msg)
        throw new IllegalArgumentException(msg)

      case Some(EBEnvironment(envName, cname, solutionStackName)) =>
        val versionLabelTask = optLabel match {
          case None =>
            (createApplicationVersion in awseb).toTask(" \"Initial Version\"")
          case Some(label) =>
            Def.task[String] { label }
        }

        Def.task[String] {
          val applicationName = (ebAppName in awseb).value
          val client = (ebClient in awseb).value
          val versionLabel = versionLabelTask.value

          val env = client.createEnvironment(
            new CreateEnvironmentRequest(applicationName, envName)
              .withCNAMEPrefix(cname)
              .withSolutionStackName(solutionStackName)
              .withVersionLabel(versionLabel))

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
          | Tier: ${env.getTier}
          | Version label: ${env.getVersionLabel}
          | ------
          |""".stripMargin)

          versionLabel
        }
    }
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
    val verLabel = CustomParsers.VersionLabel.parsed
    val applicationName = (ebAppName in awseb).value

    streams.value.log.info(s"Requesting the deletion of application version $verLabel")
    (ebClient in awseb).value.deleteApplicationVersion(
      new DeleteApplicationVersionRequest()
        .withApplicationName(applicationName)
        .withDeleteSourceBundle(true)
        .withVersionLabel(verLabel))
  }


  private def describeApplicationTask = Def.task[Unit] {
    val applicationName = (ebAppName in awseb).value
    val log = streams.value.log
    (ebClient in awseb).value.describeApplications(
      new DescribeApplicationsRequest().withApplicationNames(applicationName)
    ).getApplications.asScala.headOption match {
      case None =>
        log.warn(s"No application called ${applicationName} was found!")
      case Some(app) =>
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


  private def lookupEnvName(envMap: Map[String, EBEnvironment], alias: String): String =
    envMap.get(alias).map(_.envName).getOrElse(alias)


  private def describeConfigurationOptionsTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val envMap = (ebEnvMap in awseb).value
    val envName = lookupEnvName(envMap, alias)
    val log = streams.value.log

    (ebClient in awseb).value.describeConfigurationOptions(
      new DescribeConfigurationOptionsRequest()
        .withEnvironmentName(envName)).getOptions.asScala foreach { confOpt =>
      val builder = StringBuilder.newBuilder
      def local(label: String, obj: AnyRef): Unit = {
        if (obj ne null) {
          builder ++= label ++= ": " ++= obj.toString += '\n'
        }
      }
      builder += '\n'
      local("Change severity", confOpt.getChangeSeverity)
      local("Default value", confOpt.getDefaultValue)
      local("Max length", confOpt.getMaxLength)
      local("Max value", confOpt.getMaxValue)
      local("Min value", confOpt.getMinValue)
      local("Name", confOpt.getName)
      local("Namespace", confOpt.getNamespace)
      local("Regex", confOpt.getRegex)
      local("User defined", confOpt.getUserDefined)
      local("Value options", Option(confOpt.getValueOptions).map(_.asScala.mkString(", ")).orNull)
      local("Value type", confOpt.getValueType)
      builder ++= "------\n"

      log.info(builder.result())
    }
  }

  private def describeConfigurationSettingsTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val envMap = (ebEnvMap in awseb).value
    val envName = lookupEnvName(envMap, alias)
    val log = streams.value.log

    (ebClient in awseb).value.describeConfigurationSettings(
      new DescribeConfigurationSettingsRequest()
        .withApplicationName((ebAppName in awseb).value)
        .withEnvironmentName(envName)
    ).getConfigurationSettings.asScala foreach { confSettings =>

      log.info(s"""
      | Application name: ${confSettings.getApplicationName}
      | Date created: ${confSettings.getDateCreated}
      | Date updated: ${confSettings.getDateUpdated}
      | Deployment status: ${confSettings.getDeploymentStatus}
      | Description: ${confSettings.getDescription}
      | Environment name: ${confSettings.getEnvironmentName}
      | Solution stack name: ${confSettings.getSolutionStackName}
      | Template name: ${confSettings.getTemplateName}
      |""".stripMargin)

      confSettings.getOptionSettings.asScala foreach { optSetting =>
        log.info(s"""
        | Namespace: ${optSetting.getNamespace}
        | Option name: ${optSetting.getOptionName}
        | Value: ${optSetting.getValue}
        |""".stripMargin)
      }
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
      | Tier: ${env.getTier}
      | Version label: ${env.getVersionLabel}
      | ------
      |""".stripMargin)
    }
  }


  private def describeEnvironmentResourcesTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value
    val envName = lookupEnvName(envMap, alias)

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


  private def describeEventsTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val applicationName = (ebAppName in awseb).value
    val limit = (ebEventLimit in awseb).value
    val envMap = (ebEnvMap in awseb).value
    val envName = lookupEnvName(envMap, alias)
    val log = streams.value.log

    (ebClient in awseb).value.describeEvents(
      new DescribeEventsRequest()
        .withApplicationName(applicationName)
        .withEnvironmentName(envName)
        .withMaxRecords(limit)
    ).getEvents().asScala foreach { ev =>
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
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val envMap = (ebEnvMap in awseb).value

    val envName = lookupEnvName(envMap, alias)
    log.info(s"Requesting that environment $envName be rebuilt")
    client.rebuildEnvironment(new RebuildEnvironmentRequest().withEnvironmentName(envName))
  }


  private def requestEnvironmentInfoTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new RequestEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)
    val envMap = (ebEnvMap in awseb).value

    val envName = lookupEnvName(envMap, alias)
    log.info(s"Requesting information for environment $envName")
    client.requestEnvironmentInfo(req.withEnvironmentName(envName))
  }


  private def restartAppServerTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val envMap = (ebEnvMap in awseb).value

    val envName = lookupEnvName(envMap, alias)
    log.info(s"Requesting that app server for environment $envName be restarted")
    client.restartAppServer(new RestartAppServerRequest().withEnvironmentName(envName))
  }


  private def retrieveEnvironmentInfoTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new RetrieveEnvironmentInfoRequest().withInfoType(EnvironmentInfoType.Tail)
    val envMap = (ebEnvMap in awseb).value

    val envName = lookupEnvName(envMap, alias)
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


  private def swapEnvironmentCNAMEsTask = Def.inputTask[Unit] {
    val (alias1, alias2) = CustomParsers.EnvironmentAliasOrNamePair.parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value

    val envName1 = lookupEnvName(envMap, alias1)
    val envName2 = lookupEnvName(envMap, alias2)
    log.info(s"Requesting a swap of the CNAME from environments ${envName1} to ${envName2}")
    (ebClient in awseb).value.swapEnvironmentCNAMEs(
        new SwapEnvironmentCNAMEsRequest()
          .withSourceEnvironmentName(envName1)
          .withDestinationEnvironmentName(envName2)
      )
  }


  private def terminateEnvironmentTask = Def.inputTask[Unit] {
    val alias = CustomParsers.EnvironmentAliasOrName.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val req = new TerminateEnvironmentRequest().withTerminateResources(true)
    val envMap = (ebEnvMap in awseb).value

    val envName = lookupEnvName(envMap, alias)
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


  private def updateApplicationVersionTask = Def.inputTask[Unit] {
    val (versionLabel, versionDescription) = CustomParsers.VersionLabelAndDescription.parsed
    val log = streams.value.log
    val client = (ebClient in awseb).value
    val applicationName = (ebAppName in awseb).value

    val app = client.updateApplicationVersion(
                new UpdateApplicationVersionRequest(applicationName, versionLabel)
                  // empty string will zero out description on Beanstalk
                  .withDescription(versionDescription)
              ).getApplicationVersion()

    log.info(applicationVersionDescriptionToString(app))
  }


  private def uploadAppBundleTask = Def.taskDyn[S3Location] {
    val log = streams.value.log
    val client = (s3Client in awseb).value
    val bucketName = (s3AppBucketName in awseb).value
    val file = (ebAppBundle in awseb).value

    val checkBucketTask =
      if (client.doesBucketExist(bucketName)) Def.task[Unit] { }
      else createAppBucketTask

    Def.task[S3Location] {
      checkBucketTask.value
      log.info(s"Uploading application bundle from $file to bucket $bucketName")
      val location = S3FileUploader.upload(client, bucketName, file)
      log.info(s"$location")
      location
    }
  }


  private def updateEnvironmentVersionTask = Def.inputTaskDyn[String] {
    val (alias, optLabel) = CustomParsers.EnvironmentAliasAndOptionalVersionLabel.parsed
    val log = streams.value.log
    val envMap = (ebEnvMap in awseb).value

    envMap.get(alias) match {
      case None =>
        val msg = s"Environment alias '${alias}' was not found in the environment map"
        log.error(msg)
        throw new IllegalArgumentException(msg)
      case Some(EBEnvironment(envName, _, _)) =>
        val client = (ebClient in awseb).value

        val versionLabelTask = optLabel match {
          case None =>
            (createApplicationVersion in awseb).toTask(" \"Auto created by sbt-awseb\"")
          case Some(label) =>
            Def.task[String] { label }
        }

        Def.task[String] {
          val versionLabel = versionLabelTask.value

          log.info(s"Updating environment $envName to version label $versionLabel")
          val env = client.updateEnvironment(
            new UpdateEnvironmentRequest()
              .withEnvironmentName(envName)
              .withVersionLabel(versionLabel))

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
          | Tier: ${env.getTier}
          | Version label: ${env.getVersionLabel}
          | ------
          |""".stripMargin)

          versionLabel
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
      s3Client in awseb := s3ClientSetting.value,
      listAvailableSolutionStacks in awseb <<= listAvailableSolutionStacksTask
    )

  override lazy val projectSettings =
    Seq(
      ebAppName in awseb := moduleName.value,
      ebAppDescription in awseb := None,
      s3AppBucketName in awseb <<= Def.setting[String] { "sbt-awseb-bundle-" + (ebAppName in awseb).value },
      ebAppVersionLabel in awseb <<= Def.task[String] { s"${version.value}-${ISO8601.timestamp()}" },
      ebEnvMap in awseb := Map.empty[String, EBEnvironment],
      checkDNSAvailability in awseb <<= checkDNSAvailabilityTask,
      cleanApplicationVersions in awseb <<= cleanApplicationVersionsTask,
      createAppBucket in awseb <<= createAppBucketTask,
      createApplication in awseb <<= createApplicationTask,
      createApplicationVersion in awseb <<= createApplicationVersionTask,
      createEnvironment in awseb <<= createEnvironmentTask,
      deleteAppBucket in awseb <<= deleteAppBucketTask,
      deleteApplication in awseb <<= deleteApplicationTask,
      deleteApplicationVersion in awseb <<= deleteApplicationVersionTask,
      describeApplication in awseb <<= describeApplicationTask,
      describeApplicationVersions in awseb <<= describeApplicationVersionsTask,
      describeConfigurationOptions in awseb <<= describeConfigurationOptionsTask,
      describeConfigurationSettings in awseb <<= describeConfigurationSettingsTask,
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
      updateApplicationVersion in awseb <<= updateApplicationVersionTask,
      updateEnvironmentVersion in awseb <<= updateEnvironmentVersionTask,
      uploadAppBundle in awseb <<= uploadAppBundleTask
    )
}
