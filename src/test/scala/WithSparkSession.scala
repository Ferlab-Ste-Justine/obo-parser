import org.apache.hadoop.security.UserGroupInformation
import org.apache.spark.sql.SparkSession

trait WithSparkSession {
  UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("test"))

  implicit lazy val spark: SparkSession = SparkSession.builder()
    .config("spark.ui.enabled", value = false)
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

}
