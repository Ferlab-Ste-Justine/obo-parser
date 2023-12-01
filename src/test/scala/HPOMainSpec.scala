import bio.ferlab.HPOMain
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source


class HPOMainSpec extends AnyFlatSpec with Matchers {


  val study = "STU0000001"
  val release = "1"

  implicit val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .config("spark.ui.enabled", value = false)
    .config("fs.s3a.path.style.access", "true")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  "generateTermsWithAncestors" should "return all the terms" in {
    val file = Source.fromFile("src/test/scala/resources/hp.obo")
    val result = HPOMain.generateTermsWithAncestors(file, None)

    result.filter(t => t._1.id.startsWith("HP:2")).keySet.map(_.id) shouldEqual Set("HP:21", "HP:22")
    result.filter(t => t._1.id.startsWith("HP:3")).keySet.map(_.id) shouldEqual Set("HP:31","HP:32","HP:33","HP:34")
    result.filter(t => t._1.id.startsWith("HP:4")).keySet.map(_.id) shouldEqual Set("HP:41","HP:42","HP:43","HP:44")
  }

  "generateTermsWithAncestors" should "return only desired branch" in {
    val file = Source.fromFile("src/test/scala/resources/hp.obo")
    val result = HPOMain.generateTermsWithAncestors(file, Some("HP:22"))

    result.filter(t => t._1.id.startsWith("HP:2")).keySet.map(_.id) shouldEqual Set("HP:22")
    result.filter(t => t._1.id.startsWith("HP:3")).keySet.map(_.id) shouldEqual Set("HP:33","HP:34")
    result.filter(t => t._1.id.startsWith("HP:4")).keySet.map(_.id) shouldEqual Set("HP:43","HP:44")
  }
}
