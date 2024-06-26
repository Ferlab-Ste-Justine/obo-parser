package bio.ferlab

import bio.ferlab.config.Config
import bio.ferlab.ontology.{ICDTerm, OntologyTerm}
import bio.ferlab.transform.{DownloadTransformer, WriteJson, WriteParquet}
import org.apache.spark.sql.{SaveMode, SparkSession}
import pureconfig._
import pureconfig.generic.auto._

import scala.io.{BufferedSource, Source}

object HPOMain extends App {
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

  val Array(inputOboFileUrl, bucket, ontologyType, isICD, desiredTopNode) = args

  val outputDir = s"s3a://$bucket/$ontologyType/"

  val topNode = desiredTopNode match {
    case s if s.nonEmpty => Some(s)
    case _ => None
  }

  if(isICD.trim.toLowerCase == "true"){
    val resultICD10: List[ICDTerm] = DownloadTransformer.downloadICDFromXML(inputOboFileUrl)

    WriteJson.toJson(resultICD10)(outputDir)
  } else {
    val fileBuffer = Source.fromURL(inputOboFileUrl)
    val result = generateTermsWithAncestors(fileBuffer)

    val filteredDf = WriteParquet.filterForTopNode(result, topNode)

    filteredDf.write.mode(SaveMode.Overwrite).parquet(outputDir)
  }

def generateTermsWithAncestors(fileBuffer: BufferedSource) = {
  val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(fileBuffer)

  val mapDT = dT map (d => d.id -> d) toMap

  val dTwAncestorsParents = DownloadTransformer.addParentsToAncestors(mapDT)

  val allParents = dT.flatMap(_.parents.map(_.id))

  val ontologyWithParents = DownloadTransformer.transformOntologyData(dTwAncestorsParents)

  ontologyWithParents.map {
    case (k, v) if allParents.contains(k.id) => k -> (v, false)
    case (k, v) => k -> (v, true)
  }
}
}
