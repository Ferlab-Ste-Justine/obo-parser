package bio.ferlab.ontology

case class ICDTermConversion(
                              fromCode: Option[String] = None,
                              toCode: Option[String] = None,
                              fromChapter: String,
                              toChapter: String,
                              fromTitle: String,
                              toTitle: String
                            )