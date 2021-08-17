package bio.ferlab

import bio.ferlab.ontology.{ICDTerm, OntologyTerm}
import bio.ferlab.transform.{DownloadTransformer, WriteJson, WriteParquet}
import org.apache.spark.sql.SparkSession

object HPOMain extends App {
  implicit val spark: SparkSession = SparkSession.builder
    .appName("HPO")
    .config("spark.master", "local")
    .getOrCreate()

  val Array(inputOboFileUrl, outputDir, isICD) = args

  if(isICD.trim.toLowerCase == "true"){
    val resultICD10: List[ICDTerm] = DownloadTransformer.downloadICDFromXML(inputOboFileUrl)

    WriteJson.toJson(resultICD10)(outputDir)
  } else {
    val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(inputOboFileUrl)

    val mapDT = dT map (d => d.id -> d) toMap

    val dTwAncestorsParents = DownloadTransformer.addParentsToAncestors(mapDT)

    val allParents = dT.flatMap(_.parents.map(_.id))

    val ontologyWithParents = DownloadTransformer.transformOntologyData(dTwAncestorsParents)

    val result = ontologyWithParents.map {
      case (k, v) if allParents.contains(k.id) => k -> (v, false)
      case (k, v) => k -> (v, true)
    }

    WriteParquet.toParquet(result)(outputDir)
  }





}
