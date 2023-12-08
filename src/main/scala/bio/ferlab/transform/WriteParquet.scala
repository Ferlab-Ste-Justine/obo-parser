package bio.ferlab.transform

import bio.ferlab.ontology.OntologyTerm
import org.apache.spark.sql._
import org.apache.spark.sql.functions._


object WriteParquet {

  def filterForTopNode(data: Map[OntologyTerm, (Set[OntologyTerm], Boolean)], excludedNode: Option[String])
                      (implicit spark: SparkSession): DataFrame = {
    import spark.implicits._

    val df = data.map { case (k, v) =>
      OntologyTermOutput(
        k.id,
        k.name,
        k.parents.map(_.toString),
        v._1.map(i => BasicOntologyTermOutput(i.id, i.name, i.parents.map(_.toString))).toSeq,
        v._2
      )
    }.toSeq.toDF()

    val excludedParents = excludedNode match {
      case Some(node) => data.find { case (term, _) => term.id == node }.map { case (_, parents) => parents._1.map(_.id) }
      case None => None
    }

    (excludedParents, excludedNode) match {
      case (Some(targetParents), Some(node)) => df
        //filter out all terms not part of target root
        .where(array_contains(col("ancestors")("id"), node))
        .withColumn("ancestors_exp", explode(col("ancestors")))
        //filter out parents of target term
        .filter(!col("ancestors_exp")("id").isin(targetParents.toSeq: _*))
        // Make sure target term in ancestors do not contain any parents (it is the top root)
        .withColumn("ancestors_exp_f", when(col("ancestors_exp")("id").equalTo(node),
          struct(col("ancestors_exp")("id") as "id",
            col("ancestors_exp")("name") as "name",
            array().cast("array<string>") as "parents"))
          .otherwise(col("ancestors_exp"))
        )
        .groupBy("id", "name", "parents")
        .agg(collect_list(col("ancestors_exp_f")) as "ancestors")

      case _ => df
    }
  }
}

