package bio.ferlab

import bio.ferlab.config.Config
import bio.ferlab.ontology.{ICDTerm, OntologyTerm}
import bio.ferlab.transform.{DownloadTransformer, WriteJson, WriteParquet}
import org.apache.spark.sql.{SaveMode, SparkSession}
import pureconfig._
import pureconfig.generic.auto._
import mainargs._

import scala.io.{BufferedSource, Source}

object HPOMain {
  @main
  def run(
           @arg inputOboFileUrl: String,
           @arg ontologyType: String,
           @arg(name = "desired-top-node", short = 'n', doc = "Desired Top Node") desiredTopNode: Option[String]
          ): Unit = {

    val config =
      ConfigSource.resources("application.conf")
        .load[Config]
        .getOrElse(throw new Exception("Wrong Configuration"))

    implicit val spark: SparkSession = SparkSession.builder
      .appName("HPOMain")
      .master("local[*]")
      .config("fs.s3a.path.style.access", s"${config.aws.pathStyleAccess}")
      .config("fs.s3a.endpoint", s"${config.aws.endpoint}")
      .getOrCreate()

    val outputDir = s"s3a://${config.aws.bucketName}/${ontologyType}_terms/"

    if(ontologyType.trim.toLowerCase == "icd"){
      val resultICD10: List[ICDTerm] = DownloadTransformer.downloadICDFromXML(inputOboFileUrl)

      WriteJson.toJson(resultICD10)(outputDir)
    } else {
      val fileBuffer = Source.fromURL(inputOboFileUrl)
      val result = generateTermsWithAncestors(fileBuffer, ontologyType)

      val filteredDf = WriteParquet.filterForTopNode(result, desiredTopNode)

      filteredDf.write.mode(SaveMode.Overwrite).parquet(outputDir)
    }
  }

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrThrow(args, allowPositional = true)



def generateTermsWithAncestors(fileBuffer: BufferedSource, ontologyType: String): Map[OntologyTerm, (Set[OntologyTerm], Boolean)] = {
  val termPrefix = ontologyType match {
    case "hpo" => "HP"
    case "mondo" => "MONDO"
    case "ncid" => "NCIT"
    case "icd" => ""
    case _ => throw new IllegalArgumentException(s"Unsupported ontology type: $ontologyType")
  }

  val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(fileBuffer, termPrefix)

  val mapDT = dT.filterNot(t => t.isObsolete || t.id.isEmpty) map (d => d.id -> d) toMap

  val dTwAncestorsParents = DownloadTransformer.addParentsToAncestors(mapDT)

  val allParents = dT.flatMap(_.parents.map(_.id))

  val ontologyWithParents = DownloadTransformer.transformOntologyData(dTwAncestorsParents)

  ontologyWithParents.map {
    case (k, v) if allParents.contains(k.id) => k -> (v, false)
    case (k, v) => k -> (v, true)
  }
}
}
