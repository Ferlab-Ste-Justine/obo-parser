package bio.ferlab.ontology

case class ICDTerm(
                    eightY: Option[String] = None,
                    blockId: Option[String] = None,
                    title: String,
                    chapterNumber: String,
                    noOfNonResidualChildren: Int,
                    is_leaf: Boolean = false
                  ) {
//  override def toString: String = s"$name ($id)"
}
