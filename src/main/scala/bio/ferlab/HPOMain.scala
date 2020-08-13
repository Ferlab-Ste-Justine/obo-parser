package bio.ferlab

import bio.ferlab.ontology.OntologyTerm
import bio.ferlab.transform.{DownloadTransformer, WriteJson}
import org.apache.spark.sql.SparkSession

object HPOMain extends App {

  val Array(inputOboFileUrl, outputDir) = args

  val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(inputOboFileUrl)

  val mapDT = dT map (d => d.id -> d) toMap

  val dTwAncestorsParents = DownloadTransformer.addParentsToAncestors(mapDT)

  val allParents = dT.flatMap(_.parents.map(_.id))

  val ontologyWithParents = DownloadTransformer.transformOntologyData(dTwAncestorsParents)

  val result = ontologyWithParents.map {
    case (k, v) if allParents.contains(k.id) => k -> (v, false)
    case (k, v) => k -> (v, true)
  }

  implicit val spark: SparkSession = SparkSession.builder
    .appName("HPO")
    .getOrCreate()


  WriteJson.toJson(result)(outputDir)
}
