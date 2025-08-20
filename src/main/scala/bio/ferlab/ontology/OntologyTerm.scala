package bio.ferlab.ontology

case class OntologyTerm(
                         id: String,
                         name: String,
                         parents: Seq[OntologyTerm] = Nil,
                         is_leaf: Boolean = false,
                         alternateIds: Seq[String] = Nil,
                         isObsolete: Boolean = false,
                       ) {
  override def toString: String = s"$name ($id)"
}


case class FlatOntologyTerm(
                             id: String,
                             name: String,
                             parents: Seq[String],
                             is_leaf: Boolean,
                             alternateIds: Seq[String],
                             isObsolete: Boolean
                           ) {
  override def toString: String = s"$name ($id)"
}