package bio.ferlab.ontology

case class ICDTerm(
                    eightY: Option[String] = None,
                    title: String,
                    chapterNumber: String,
                    is_leaf: Boolean = false,
                    parents: Seq[ICDTerm] = Nil
                  ) {
//  override def toString: String = s"$title" + s"${if(eightY.isDefined) " (" + eightY.get + ")" else ""}"
}

object ICDTerm {
  def apply (eightY: Option[String], title: String): ICDTerm = {
    new ICDTerm(
      eightY = eightY,
      title = title,
      chapterNumber = "",
    )
  }
}
