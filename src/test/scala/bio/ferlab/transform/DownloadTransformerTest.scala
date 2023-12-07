package bio.ferlab.transform

import bio.ferlab.ontology.{ICDTerm, OntologyTerm}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DownloadTransformerTest extends AnyFlatSpec with Matchers  {

  val a0: OntologyTerm = OntologyTerm("A0", "NameA0") //root

  val a1: OntologyTerm = OntologyTerm("A1", "NameA1", parents = Seq(OntologyTerm("A0", "NameA0")))
  val a2: OntologyTerm = OntologyTerm("A2", "NameA2", parents = Seq(OntologyTerm("A0", "NameA0")))

  val a11: OntologyTerm = OntologyTerm("A11", "NameA11", parents = Seq(OntologyTerm("A1", "NameA1")))
  val a12: OntologyTerm = OntologyTerm("A12", "NameA12", parents = Seq(OntologyTerm("A1", "NameA1")))
  val a21: OntologyTerm = OntologyTerm("A21", "NameA21", parents = Seq(OntologyTerm("A2", "NameA2")))

  val a111: OntologyTerm = OntologyTerm("A111", "NameA111", parents = Seq(OntologyTerm("A11", "NameA11")))
  val a112: OntologyTerm = OntologyTerm("A112", "NameA112", parents = Seq(OntologyTerm("A11", "NameA11")))
  val a121: OntologyTerm = OntologyTerm("A121", "NameA121", parents = Seq(OntologyTerm("A12", "NameA12")))
  val a122: OntologyTerm = OntologyTerm("A122", "NameA122", parents = Seq(OntologyTerm("A12", "NameA12")))
  val a221: OntologyTerm = OntologyTerm("A211", "NameA211", parents = Seq(OntologyTerm("A21", "NameA21"), OntologyTerm("A12", "NameA12")))

  val seq: List[OntologyTerm] = List(a0, a1, a2, a11, a12, a21, a111, a112, a121, a122, a221)
  val data: Map[String, OntologyTerm] = seq map (i => i.id -> i) toMap

  "loadTerms" should "load ontological terms from compressed TSV file" in {
    val result = DownloadTransformer.transformOntologyData(data)

    result should contain theSameElementsAs Map(
      OntologyTerm("A1", "NameA1", parents = Seq(OntologyTerm("A0", "NameA0"))) -> Set(OntologyTerm("A0", "NameA0")),
      OntologyTerm("A2", "NameA2", parents = Seq(OntologyTerm("A0", "NameA0"))) -> Set(OntologyTerm("A0", "NameA0")),
      OntologyTerm("A11", "NameA11", parents = Seq(OntologyTerm("A1", "NameA1")))-> Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A12", "NameA12", parents = Seq(OntologyTerm("A1", "NameA1")))-> Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A21", "NameA21", parents = Seq(OntologyTerm("A2", "NameA2")))-> Set(OntologyTerm("A2", "NameA2"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A111", "NameA111", parents = Seq(OntologyTerm("A11", "NameA11")))->
        Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A11", "NameA11"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A112", "NameA112", parents = Seq(OntologyTerm("A11", "NameA11")))->
        Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A11", "NameA11"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A121", "NameA121", parents = Seq(OntologyTerm("A12", "NameA12")))->
        Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A12", "NameA12"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A122", "NameA122", parents = Seq(OntologyTerm("A12", "NameA12")))->
        Set(OntologyTerm("A1", "NameA1"), OntologyTerm("A12", "NameA12"), OntologyTerm("A0", "NameA0")),
      OntologyTerm("A211", "NameA211", parents = Seq(OntologyTerm("A21", "NameA21"), OntologyTerm("A12", "NameA12"))) ->
        Set(
          OntologyTerm("A2", "NameA2"),
          OntologyTerm("A21", "NameA21"),
          OntologyTerm("A0", "NameA0"),
          OntologyTerm("A12", "NameA12"),
          OntologyTerm("A1", "NameA1")
        )
    )
  }


  "downloadICDs" should "load ICDs form an excel file" in {
//    val inputURL = "https://icd.who.int/browse11/Downloads/Download?fileName=simpletabulation.zip"
    val inputURL = "../obo-parser/src/main/resources/ICD-11-SimpleTabulation.xlsx"
//    val inputURLConversion = "../obo-parser/src/main/resources/11To10MapToOneCategory.xlsx"

    val resultICD11 = DownloadTransformer.downloadICDs(inputURL)

    val expectedTerm11 = Some(ICDTerm(
      eightY = Some("1A07.Y"),
      title = "Other specified typhoid fever",
      chapterNumber = "01",
      is_leaf = true,
      parent = Some(ICDTerm(
        eightY = Some("1A07"), title = "Typhoid fever", chapterNumber = "01"
      )),
      ancestors = Seq(
        ICDTerm(eightY = Some("1A07"), title = "Typhoid fever", chapterNumber = "01"),
        ICDTerm(eightY = None, title = "Bacterial intestinal infections", chapterNumber = "01"),
        ICDTerm(eightY = None, title = "Gastroenteritis or colitis of infectious origin", chapterNumber = "01"),
        ICDTerm(eightY = None, title = "Certain infectious or parasitic diseases", chapterNumber = "01"),
      )
    ))


    val testICD11Term = resultICD11.find(r => r.title == expectedTerm11.get.title)

    testICD11Term should equal(expectedTerm11)
  }

  //https://www.oandp.com/opie/help/manuals/opie/index.html#!downloadorupdatetheicd10diagnosiscodelist.htm
  "downloadICDs" should "load ICDs 10 form an XML file" in {
    val inputURL = "../obo-parser/src/main/resources/icd10cm_tabular_2021.xml"

    val resultICD10 = DownloadTransformer.downloadICDFromXML(inputURL)

    val expectedTerm11 = Some(ICDTerm(
      eightY = Some("C00.0"),
      title = "Malignant neoplasm of external upper lip",
      chapterNumber = "2",
      is_leaf = true,
      parent = Some(ICDTerm(
        eightY = Some("C00"), title = "Malignant neoplasm of lip", chapterNumber = "2"
      )),
      ancestors = Seq(
        ICDTerm(eightY = None, title = "Neoplasms", chapterNumber = "2"),
        ICDTerm(eightY = Some("C00-C14"), title = "Malignant neoplasms of lip, oral cavity and pharynx", chapterNumber = "2"),
        ICDTerm(eightY = Some("C00"), title = "Malignant neoplasm of lip", chapterNumber = "2"),
      )
    ))

    val testICD10Term = resultICD10.find(r => r.title == expectedTerm11.get.title)

    testICD10Term should equal(expectedTerm11)
  }

}
