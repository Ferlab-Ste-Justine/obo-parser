package bio.ferlab.transform

import bio.ferlab.ontology.OntologyTerm
import org.apache.spark.sql._


object WriteParquet {

  def toParquet(data: Map[OntologyTerm, (Set[OntologyTerm], Boolean)])(outputDir: String)(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    data.map{ case(k, v) =>
      OntologyTermOutput(
        k.id,
        k.name,
        k.parents.map(_.toString),
        v._1.map(i => BasicOntologyTermOutput(i.id, i.name, i.parents.map(_.toString))).toSeq,
        v._2
      )}.toSeq.toDF().write.mode(SaveMode.Overwrite).parquet(outputDir)
  }
}

