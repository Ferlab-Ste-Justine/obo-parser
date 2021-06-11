package bio.ferlab.ontology

case class ICDTerm(
                    eightY: Option[String] = None,
                    title: String,
                    chapterNumber: String,
                    is_leaf: Boolean = false,
                    parent: Option[ICDTerm] = None,
                    ancestors: Seq[ICDTerm] = Nil
                  ) {
  def copyLight: ICDTerm = ICDTerm(this.eightY, this.title, this.chapterNumber)
  override def toString: String = s"$title" + s"${if(eightY.isDefined) " (" + eightY.get + ")" else ""}"
}

object ICDTerm {
  def apply (eightY: Option[String], title: String, chapterNumber: String): ICDTerm = {
    new ICDTerm(
      eightY = eightY,
      title = title,
      chapterNumber = chapterNumber
    )
  }
}
