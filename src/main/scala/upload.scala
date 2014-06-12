package awseb

import com.amazonaws.event.{ ProgressEvent, ProgressListener }
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.elasticbeanstalk.model.S3Location

import java.io.File

object S3FileUploader {

  def upload(s3: AmazonS3, bucketName: String, file: File): S3Location = {
    val key = s"${ISO8601.timestamp()}-${Integer.toString(util.Random.nextInt(), 36)}-${file.getName}"

    val location = new S3Location(bucketName, key)

    val transferManager = new TransferManager(s3)
    val upload = transferManager.upload(bucketName, key, file)

    upload.addProgressListener(new ProgressListener() {
      def progressChanged(event: ProgressEvent) {
        print("\rTransferred: (%.1f%%)".format(upload.getProgress.getPercentTransferred))
      }
    })

    upload.waitForUploadResult()
    println()
    println(s"Transfer: ${upload.getState}")
    location
  }
}
