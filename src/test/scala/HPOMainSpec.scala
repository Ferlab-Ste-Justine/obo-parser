import bio.ferlab.HPOMain.{generateTermsWithAncestors, removeObsoleteTerms}
import bio.ferlab.ontology.OntologyTerm
import bio.ferlab.transform.{DownloadTransformer, OntologyTermOutput, WriteParquet}
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

  import spark.implicits._

  spark.sparkContext.setLogLevel("ERROR")

  "generateTermsWithAncestors" should "return all the terms" in {
    val fileBuffer = Source.fromFile("src/test/scala/resources/hp.obo")

    val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(fileBuffer, "HP")
    val clearDT = removeObsoleteTerms(dT)

    val termWithAncestors = generateTermsWithAncestors(clearDT)

    val filteredDf = WriteParquet.filterForTopNode(termWithAncestors, None)

    val result = filteredDf.as[OntologyTermOutput].collect().toSeq

    result.map(_.id) should contain theSameElementsAs Set("HP:21", "HP:22", "HP:31", "HP:32", "HP:33", "HP:34", "HP:41", "HP:42", "HP:43", "HP:44")
  }

  "generateTermsWithAncestors" should "return only desired branch" in {
    val fileBuffer = Source.fromFile("src/test/scala/resources/hp.obo")

    val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(fileBuffer, "HP")
    val clearDT = removeObsoleteTerms(dT)

    val termWithAncestors = generateTermsWithAncestors(clearDT)

    val filteredDf = WriteParquet.filterForTopNode(termWithAncestors, Some("HP:22"))

    val result = filteredDf.as[OntologyTermOutput].collect().toSeq

    result.map(_.id) should contain theSameElementsAs Set("HP:33", "HP:34", "HP:43", "HP:44")

    val hp4 = result.find(e => e.id == "HP:44").get
    hp4.ancestors.map(_.id) should contain theSameElementsAs Seq("HP:22", "HP:34")
  }

}
