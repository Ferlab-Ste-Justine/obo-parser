package bio.ferlab

import bio.ferlab.config.Config
import bio.ferlab.ontology.{FlatOntologyTerm, OntologyTerm}
import bio.ferlab.transform.{DownloadTransformer, WriteParquet}
import mainargs._
import org.apache.spark.sql.functions.{col, collect_list, explode_outer}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import pureconfig._
import pureconfig.generic.auto._

import scala.io.Source
import scala.xml.Elem

object HPOMain {
  @main
  def run(
           @arg(name = "file-url", short = 'f', doc = "File Url") inputFileUrl: String,
           @arg(name = "ontology-type", short = 't', doc = "Ontology Type") ontologyType: String,
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
      .config("fs.s3a.access.key", s"${config.aws.accessKey}")
      .config("fs.s3a.secret.key", s"${config.aws.secretKey}")
      .config("fs.s3a.path.style.access", "true")
      .getOrCreate()

    val outputDir = s"s3a://${config.aws.datalakeBucket}/${ontologyType}_terms/"

    val termPrefix = ontologyType match {
      case "hpo" => "HP"
      case "mondo" => "MONDO"
      case "ncit" => "NCIT"
      case "icd" => ""
      case _ => throw new IllegalArgumentException(s"Unsupported ontology type: $ontologyType")
    }

    val dT = ontologyType.trim.toLowerCase match {
      case "icd" =>
        val xmlString = spark.read.textFile(s"s3a://$inputFileUrl").collect().mkString("\n")
        val xml = scala.xml.XML.loadString(xmlString)

        DownloadTransformer.downloadICDFromXML(xml: Elem)

      case _ =>
        val fileBuffer = Source.fromURL(inputFileUrl)
        val dT: Seq[OntologyTerm] = DownloadTransformer.downloadOntologyData(fileBuffer, termPrefix)
        removeObsoleteTerms(dT)
    }

    val result = generateTermsWithAncestors(dT)

    val filteredDf = WriteParquet.filterForTopNode(result, desiredTopNode)
    filteredDf.write.mode(SaveMode.Overwrite).parquet(outputDir)
  }

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrThrow(args, allowPositional = true)



  def removeObsoleteTerms(dT: Seq[OntologyTerm])(implicit spark: SparkSession): Seq[OntologyTerm] = {
    import spark.implicits._
    val flatDT = dT.map(t => FlatOntologyTerm(id = t.id, name = t.name, parents = t.parents.map(_.id), is_leaf = t.is_leaf, alternateIds = t.alternateIds, isObsolete = t.isObsolete))
    val flatDF = flatDT.toDF()

    val nonObsoleteDF = flatDF.filter($"isObsolete" === false && $"id" =!= "")
    val obsoleteIdsDF = flatDF.filter($"isObsolete" === true).select($"id".as("obsolete_id"))

    val nonObsoleteDFExp = nonObsoleteDF.withColumn("parent", explode_outer($"parents")).drop($"parents")

    val cleanedDF = nonObsoleteDFExp
      .join(obsoleteIdsDF, nonObsoleteDFExp("parent") === obsoleteIdsDF("obsolete_id"), "left_anti")

    val groupCols = cleanedDF.columns.filter(_ != "parent").map(col)
    val resultDF = cleanedDF
      .groupBy(groupCols: _*)
      .agg(collect_list($"parent").as("parents"))

    val rows = resultDF.collect()
    val termMap: Map[String, (String, Boolean, Seq[String], Boolean)] = rows.map {
      case Row(id: String, name: String, is_leaf: Boolean, alternateIds: Seq[String], isObsolete: Boolean, _: Seq[String]) =>
        id -> (name, is_leaf, alternateIds, isObsolete)
    }.toMap

    rows.map {
      case Row(id: String, name: String, is_leaf: Boolean, alternateIds: Seq[String], isObsolete: Boolean, parents: Seq[String]) =>
        val parentTerms = parents.map { pid =>
          val (pname, pleaf, paltIds, pisObsolete) = termMap.getOrElse(pid, ("", false, Seq.empty, false))
          OntologyTerm(id = pid, name = pname, parents = Seq.empty, is_leaf = pleaf, alternateIds = paltIds, isObsolete = pisObsolete)
        }
        OntologyTerm(id = id, name = name, parents = parentTerms, is_leaf = is_leaf, alternateIds = alternateIds, isObsolete = isObsolete)
    }.toSeq
  }

  def generateTermsWithAncestors(dT: Seq[OntologyTerm])(implicit spark: SparkSession): Map[OntologyTerm, (Set[OntologyTerm], Boolean)] = {

    val mapDT = dT.map(d => d.id -> d).toMap
    val allParents = dT.flatMap(_.parents.map(_.id))
    val dTwAncestorsParents = DownloadTransformer.addParentsToAncestors(mapDT)
    val ontologyWithParents = DownloadTransformer.transformOntologyData(dTwAncestorsParents)

    ontologyWithParents.map {
      case (k, v) if allParents.contains(k.id) => k -> (v, false)
      case (k, v) => k -> (v, true)
    }
  }
}
