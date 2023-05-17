package bio.ferlab.transform

import bio.ferlab.ontology.{ICDTerm, OntologyTerm}
import org.apache.spark.sql._


case class OntologyTermOutput (
                                id: String,
                                name: String,
                                parents: Seq[String] = Nil,
                                ancestors: Seq[BasicOntologyTermOutput] = Nil,
                                is_leaf: Boolean =false,
                                alt_ids: Seq[String] = Nil
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
        k.parents.map(_.toString),
        v._1.map(i => BasicOntologyTermOutput(i.id, i.name, i.parents.map(_.toString))).toSeq,
        v._2,
        k.alternateIds
      )}.toSeq.toDF().write.mode("overwrite").json(outputDir)
  }

  def toJson(data: List[ICDTerm])(outputDir: String)(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    data.map(t =>
      OntologyTermOutput(
        t.eightY.getOrElse("") + "|" + t.chapterNumber,
        t.title,
        t.parent match {
          case Some(parent) => Seq(parent.toString)
          case None => Nil
        },
        t.ancestors.map(i => BasicOntologyTermOutput(i.eightY.getOrElse(""), i.title)),
        t.is_leaf
      )
    ).toDF().write.mode("overwrite").json(outputDir)
  }
}

