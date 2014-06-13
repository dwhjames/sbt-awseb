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

#### See

- [Settings](MANUAL.md#settings)

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

#### Note

The `awseb::uploadAppBundle` task will be run to first upload a new application
bundle.

#### See

- [Upload Application Bundle](MANUAL.md#upload-application-bundle)
- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CreateApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#createApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest)">Java SDK</a>

---


### Create Environment

Create an Elastic Beanstalk environment.

```
awseb::createEnvironment <environment alias> [<version label>]
```

This task requires one of the environment aliases defined in the environment map.
Optionally it will also take a version label to use as the application version to
deploy the environment with. If a version label is not supplied, the
`awseb::createApplicationVersion` task is run first to create a new application version.

#### Note

The environment aliases are defined in setting `awseb::ebEnvMap`.

#### See

- [Create Application Version](MANUAL.md#create-application-version)
- [Settings](MANUAL.md#settings)
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

#### See

- [Settings](MANUAL.md#settings)

---


### Delete Application

Delete the Elastic Beanstalk application.

```
awseb::deleteApplication
```

#### Note

You cannot delete an application that has a running environment.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DeleteApplication.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#deleteApplication(com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest)">Java SDK</a>

---


### Delete Application Version

Delete the specified application version.

```
awseb::deleteApplicationVersion <version label>
```

#### Note

- You cannot delete an application version that is associated with a running environment.
- The application bundle will also be removed from the S3 bucket.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DeleteApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#deleteApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest)">Java SDK</a>

---


### Describe Application

Describe the application

```
awseb::describeApplication
```

This will display metadata about the application.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeApplications.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeApplications(com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest)">Java SDK</a>

---


### Describe Application Versions

Describe the application versions.

```
awseb::describeApplicationVersions
```

This will display metadata about every version associated with the application.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeApplicationVersions.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeApplicationVersions(com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest)">Java SDK</a>

---


### Describe Configuration Options

Describe the configuration options that are available for the specified environment.

```
awseb::describeConfigurationOptions <environment alias or name>
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeConfigurationOptions.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeConfigurationOptions(com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest)">Java SDK</a>

---


### Describe Configuration Settings

Describe the values of the configuration options that are set for the specified environment.

```
awseb::describeConfigurationSettings <environment alias or name>
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeConfigurationSettings.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeConfigurationSettings(com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest)">Java SDK</a>

---


### Describe Environments

Describe all the environments that are associated with the application.

```
awseb::describeEnvironments
```

This will display metadata and operational information about each environment.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEnvironments.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEnvironments(com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest)">Java SDK</a>

---


### Describe Environment Resources

Describe all the AWS resources that are used by the specified environment.

```
awseb::describeEnvironmentResources <environment alias or name>
```

This will display the identities of all Elastic Beanstalk related AWS resources that are used by the environment.
This includes autoscaling groups, instances, etc. (The Cloud Formation stack may include additional resources.)


#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEnvironmentResources.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEnvironmentResources(com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest)">Java SDK</a>

---


### Describe Events

Describe the recent event stream of the specified environment.

```
awseb::describeEvents <environment alias or name>
```

This will display each event and its metadata in reverse chronological order.

#### Note

The number of events returned is controlled by the setting `awseb::ebEventLimit`.

#### See

- [Settings](MANUAL.md#settings)
- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_DescribeEvents.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#describeEvents(com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest)">Java SDK</a>

---


### List Available Solution Stacks

List the names of all the available solution stacks.

```
awseb::listAvailableSolutionStacks
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_ListAvailableSolutionStacks.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#listAvailableSolutionStacks(com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksRequest)">Java SDK</a>

---


### Rebuild Environment

Request that the specified environment be rebuilt.

```
awseb::rebuildEnvironment <environment alias or name>
```

#### Note

This task will complete once the request has been made and the
environment will rebuild asynchronously in the background.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RebuildEnvironment.html)
- <a href="">Java SDK</a>

---


### Request Environment Info

Request the tail of the instance logs for the specified environment.

```
awseb::requestEnvironmentInfo <environment alias or name>
```

At a later time the `awseb::retrieveEnvironmentInfo` task can be invoked
to view the logs.

#### Note

This task will complete once the request has been made and the
log tails will be collected asynchronously in the background.

#### See

- [Retrieve Environment Info](MANUAL.md#retrieve-environment-info)
- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RequestEnvironmentInfo.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#rebuildEnvironment(com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest)">Java SDK</a>

---


### Restart App Server

Request that the app servers in the specified environment be restarted.

```
awseb::restartAppServer <environment alias or name>
```

#### Note

This task will complete once the request has been made and the
servers will restart in the background.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RestartAppServer.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#restartAppServer(com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest)">Java SDK</a>

---


### Retrieve Environment Info

Retrieve the tail of the instance logs for the specified environment.

```
awseb::retrieveEnvironmentInfo <environment alias or name>
```

The `awseb::requestEnvironmentInfo` task should have been invoked prior to this task.

#### See

- [Request Environment Info](MANUAL.md#request-environment-info)
- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_RetrieveEnvironmentInfo.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#retrieveEnvironmentInfo(com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest)">Java SDK</a>

---


### Swap Environment CNAMEs

Swap the CNAMEs of two environments.

```
awseb::swapEnvironmentCNAMEsTask <environment alias or name> <environment alias or name>
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_SwapEnvironmentCNAMEs.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#swapEnvironmentCNAMEs()">Java SDK</a>

---


### Terminate Environment

Request that the specified environment be terminated.

```
awseb::terminateEnvironment <environment alias or name>
```

#### Note

This task will complete once the request has been made and the
environment will terminate asynchronously in the background.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_TerminateEnvironment.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#terminateEnvironment(com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest)">Java SDK</a>

---


### Update Application

Update the application to use the description defined by setting key `awseb::ebAppDescription`.

```
awseb::updateApplication
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateApplication.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateApplication(com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationRequest)">Java SDK</a>

---


### Update Application Version

Update the specified application version to use the specified description.

```
awseb::updateApplicationVersion <version label> <version description>
```

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateApplicationVersion.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateApplicationVersion(com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionRequest)">Java SDK</a>

---


### Upload Application Bundle

Take the application bundle file that was generated locally and upload it to S3.

```
awseb::uploadAppBundle
```

#### Note

- The bucket where the bundle will be stored is defined by setting key `awseb::s3AppBucketName`.
- The bundle file is generated by the task `awseb::ebAppBundle`.

#### See

- [Settings](MANUAL.md#settings)
- [Generate Application Bundle](MANUAL.md#generate-application-bundle)


---

### Update Environment Version

Update the specified environment to use the specified application version.

```
awseb::updateEnvironmentVersion <environment alias> [<version label>]
```

This task requires one of the environment aliases defined in the environment map.
Optionally it will also take a version label to use as the application version to
update the environment with. If a version label is not supplied, the
`awseb::createApplicationVersion` task is run first to create a new application version.

#### Note

The environment aliases are defined in setting `awseb::ebEnvMap`.

#### See

- [API Reference](http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html)
- <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html#updateEnvironment(com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest)">Java SDK</a>

---
