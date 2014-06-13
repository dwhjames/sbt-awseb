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

---


### Create Application Bucket

Create an S3 bucket in which to store application bundles.

```
awseb::createAppBucket
```

#### Note

The bucket name is set by the key `awseb::s3AppBucketName`.

---


### Create Application

Create an Elastic Beanstalk application

```
awseb::createApplication
```

#### See

---


### Create Application Version

Create an Elastic Beanstalk application version.

```
awseb::createApplicationVersion <version description>
```

This task returns the version label of the application version created.

#### See

---


### Create Environment

Create an Elastic Beanstalk environment.

```
awseb::createEnvironment <environment alias>
```

#### Note

The environment aliases are defined in setting `awseb::ebEnvMap`.

#### See

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

---


### Delete Application Version

---


### Describe Application

---


### Describe Application Versions

---


### Describe Configuration Options

---


### Describe Configuration Settings

---


### Describe Environments

---


### Describe Environment Resources

---


### Describe Events

---


### List Available Solution Stacks

---


### Rebuild Environment

---


### Request Environment Info

---


### Restart App Server

---


### Retrieve Environment Info

---


### Swap Environment CNAMEs

---


### Terminate Environment

---


### Update Application

---


### Update Application Version

---


### Upload App Bundle

---

### Update Environment Version

---
