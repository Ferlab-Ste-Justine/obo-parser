package bio.ferlab.transform

import bio.ferlab.ontology.OntologyTerm
import org.apache.spark.sql._


case class OntologyTermOutput (
                                id: String,
                                name: String,
                                parents: Seq[BasicOntologyTermOutput] = Nil,
                                ancestors: Seq[BasicOntologyTermOutput] = Nil,
                                is_leaf: Boolean =false
                              ) {}

case class BasicOntologyTermOutput (
                                   id: String,
                                   name: String,
                                   parents: Seq[String] = Nil
                                   ){
  override def toString: String = s"$name ($id)"
}


object WriteJson {

  def toJson(data: Map[OntologyTerm, (Set[OntologyTerm], Boolean)])(outputDir: String)(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    data.map{ case(k, v) =>
      OntologyTermOutput(
        k.id,
        k.name,
        k.parents.map(i => BasicOntologyTermOutput(i.id, i.name, i.parents.map(_.toString))),
        v._1.map(i => BasicOntologyTermOutput(i.id, i.name, i.parents.map(_.toString))).toSeq,
        v._2
      )}.toSeq.toDF().write.mode("overwrite").json(outputDir)
  }
}

