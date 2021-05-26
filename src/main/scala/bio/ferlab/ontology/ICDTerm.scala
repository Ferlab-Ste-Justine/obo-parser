package bio.ferlab.ontology

case class ICDTerm(
                    eightY: Option[String] = None,
                    title: String,
                    chapterNumber: String,
                    noOfNonResidualChildren: Int,
                    is_leaf: Boolean = false,
                    parents: Seq[String] = Nil
                  ) {
//  override def toString: String = s"$name ($id)"
}
