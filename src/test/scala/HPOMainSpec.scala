import bio.ferlab.HPOMain
import bio.ferlab.transform.WriteParquet
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{SparkSession, functions}
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

  import spark.implicits._

  spark.sparkContext.setLogLevel("ERROR")

  "generateTermsWithAncestors" should "return all the terms" in {
    val file = Source.fromFile("src/test/scala/resources/hp.obo")
    val hpoAncestors = HPOMain.generateTermsWithAncestors(file)
    val filteredDf = WriteParquet.filterForTopNode(hpoAncestors, None)
    val result = filteredDf.select(functions.col("id")).as[String].collect()

    result should contain theSameElementsAs Set("HP:21", "HP:22", "HP:31", "HP:32", "HP:33", "HP:34", "HP:41", "HP:42", "HP:43", "HP:44")
  }

  "generateTermsWithAncestors" should "return only desired branch" in {
    val file = Source.fromFile("src/test/scala/resources/hp.obo")
    val hpoAncestors = HPOMain.generateTermsWithAncestors(file)
    val filteredDf = WriteParquet.filterForTopNode(hpoAncestors, Some("HP:22"))
    val result = filteredDf.select(functions.col("id")).as[String].collect()


    val test = filteredDf.select(col("id"), col("ancestors")("id")).as[(String, Seq[String])].collect

    result should contain theSameElementsAs Set("HP:33", "HP:34", "HP:43", "HP:44")

    val hp4 = test.find(_._1 == "HP:44").get

    hp4._2 should contain theSameElementsAs Seq("HP:22", "HP:34")
  }
}
