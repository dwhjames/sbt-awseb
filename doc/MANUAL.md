# sbt-awseb Manual

## Settings

<dl>
  <dt><tt>awseb::ebAppName</tt></dt>
  <dd>The name of the Elastic Beanstalk application, defaults to the module name.</dd>
  <dt><tt>awseb::ebAppDescription</tt></dt>
  <dd>The description of the Elastic Beanstalk application, defaults to None.</dd>
  <dt><tt>awseb::ebEnvMap</tt></dt>
  <dd>The map of environments for the Elastic Beanstalk application, defaults to empty.</dd>
  <dt><tt>awseb::ebEventLimit</tt></dt>
  <dd>The limit for the number of recent events to list, defaults 10.</dd>
  <dt><tt>awseb::ebRegion</tt></dt>
  <dd>The AWS region in which to host the Elastic Beanstalk application, defaults to US East 1.</dd>
  <dt><tt>awseb::s3AppBucketName</tt></dt>
  <dd>The name of the S3 bucket in which to store application bundles, defaults to the sbt-awseb-bundle- + the value of <tt>awseb::ebAppName</tt>.</dd>
</dl>

---

### Advanced Settings

<dl>
  <dt><tt>awsCredentialsProvider</tt></dt>
  <dd>The AWS credentials provider to use, defaults to <tt>ProfileCredentialsProvider</tt>.</dd>
  <dt><tt>awsCredentialsProfileName</tt></dt>
  <dd>The profile name to configure the profile credentials provider, defaults to None which selects the default profile.</dd>
</dl>

## Tasks

### Generate Application Bundle

The `awseb::ebAppBundle` task key must be defined with a task that produces a `java.io.File` pointing to the application bundle.

---


### Generate Application Version Label

The `awseb::ebAppVersionLabel` task key generates a fresh label to use as a version label.

By default this is the value of the sbt `version` key, with an ISO8601 timestamp suffix.

---


## AWS Beanstalk Tasks


### Check DNS Availability

Check if the specified CNAME is available.

```
awseb::checkDNSAvailability <CNAME>
```

#### See
- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CheckDNSAvailability.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#checkDNSAvailability(com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest)">Java SDK</a>

---


### Clean Application Versions

Remove the unused application versions.

```
awseb::cleanApplicationVersions
```

This task removes all application versions that are not currently deployed to an environment for this application.

#### See
- [Delete Application Version](MANUAL.md#delete-application-version)

---


### Create Application Bucket

Create an S3 bucket in which to store application bundles.

```
awseb::createAppBucket
```

#### Note

- The bucket name is set by the key `awseb::s3AppBucketName`.
- The bucket must exist before any application versions can be created.

---


### Create Application

Create an Elastic Beanstalk application

```
awseb::createApplication
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CreateApplication.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#createApplication(com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest)">Java SDK</a>
---


### Create Application Version

Create an Elastic Beanstalk application version.

```
awseb::createApplicationVersion <version description>
```

This task returns the version label of the application version created.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CreateApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#createApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest)">Java SDK</a>

---


### Create Environment

Create an Elastic Beanstalk environment.

```
awseb::createEnvironment <environment alias>
```

#### Note

The environment aliases are defined in setting `awseb::ebEnvMap`.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#createEnvironment(com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest)">Java SDK</a>

---


### Delete Application Bucket

Delete the S3 bucket that stores application bundles.

```
awseb::deleteAppBucket
```

This will only succeed for a bucket that is empty.

#### Note

The bucket name is set by the key `awseb::s3AppBucketName`.

---


### Delete Application

Delete the Elastic Beanstalk application.

```
awseb::deleteApplication
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DeleteApplication.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#deleteApplication(com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest)">Java SDK</a>

---


### Delete Application Version

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DeleteApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#deleteApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest)">Java SDK</a>

---


### Describe Application

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeApplications.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeApplications(com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest)">Java SDK</a>

---


### Describe Application Versions

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeApplicationVersions.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeApplicationVersions(com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest)">Java SDK</a>

---


### Describe Configuration Options

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeConfigurationOptions.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeConfigurationOptions(com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest)">Java SDK</a>

---


### Describe Configuration Settings

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeConfigurationSettings.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeConfigurationSettings(com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest)">Java SDK</a>

---


### Describe Environments

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEnvironments.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEnvironments(com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest)">Java SDK</a>

---


### Describe Environment Resources

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEnvironmentResources.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEnvironmentResources(com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest)">Java SDK</a>

---


### Describe Events

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEvents.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEvents(com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest)">Java SDK</a>

---


### List Available Solution Stacks

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_ListAvailableSolutionStacks.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#listAvailableSolutionStacks(com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksRequest)">Java SDK</a>

---


### Rebuild Environment

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RebuildEnvironment.html)
- <a href="">Java SDK</a>

---


### Request Environment Info

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RequestEnvironmentInfo.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#rebuildEnvironment(com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest)">Java SDK</a>

---


### Restart App Server

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RestartAppServer.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#restartAppServer(com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest)">Java SDK</a>

---


### Retrieve Environment Info

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RetrieveEnvironmentInfo.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#retrieveEnvironmentInfo(com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest)">Java SDK</a>

---


### Swap Environment CNAMEs

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_SwapEnvironmentCNAMEs.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#swapEnvironmentCNAMEs()">Java SDK</a>

---


### Terminate Environment

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_TerminateEnvironment.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#terminateEnvironment(com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest)">Java SDK</a>

---


### Update Application

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateApplication.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateApplication(com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationRequest)">Java SDK</a>

---


### Update Application Version

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionRequest)">Java SDK</a>

---


### Upload App Bundle


---

### Update Environment Version

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateEnvironment(com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest)">Java SDK</a>

---
