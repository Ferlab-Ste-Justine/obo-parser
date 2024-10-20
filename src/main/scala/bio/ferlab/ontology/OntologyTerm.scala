package bio.ferlab.ontology

case class OntologyTerm(
                         id: String,
                         name: String,
                         parents: Seq[OntologyTerm] = Nil,
                         is_leaf: Boolean = false,
                         alternateIds: Seq[String] = Nil
                       ) {
  override def toString: String = s"$name ($id)"
}
