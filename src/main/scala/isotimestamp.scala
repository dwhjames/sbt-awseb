package awseb

import java.text.SimpleDateFormat
import java.util.{ Date, TimeZone }

object ISO8601 {
  def timestamp(): String = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    df.format(new Date());
  }
}
