package bio.ferlab.transform

import bio.ferlab.ontology.{ICDTerm, ICDTermConversion, OntologyTerm}
import org.apache.poi.ss.usermodel.{Cell, CellType, Row, Sheet, WorkbookFactory}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io.{File, FileInputStream}
import java.util.zip.ZipFile
import javax.print.DocFlavor.URL
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

object DownloadTransformer {
  val patternId = "id: ([A-Z]+:[0-9]+)".r
  val patternName = "name: (.*)".r
  val patternIsA = "is_a: ([A-Z]+:[0-9]+) (\\{.*})? ?! (.*)".r

  def using[A](r: BufferedSource)(f: BufferedSource => A): A =
    try {
      f(r)
    }
    finally {
      r.close()
    }

  def downloadOntologyData(inputFileUrl: String): List[OntologyTerm] = {
    val file = readTextFileWithTry(inputFileUrl)
    file match {
      case Success(lines) => lines.foldLeft(List.empty[OntologyTerm]) { (current, line) =>
        if (line.trim == "[Term]") {
          OntologyTerm("", "") :: current
        } else if (line.matches(patternId.regex)) {
          val patternId(id) = line
          val headOnto = current.head
          headOnto.copy(id = id) :: current.tail
        } else if (line.matches(patternName.regex)) {
          val patternName(name) = line
          val headOnto = current.head
          headOnto.copy(name = name) :: current.tail
        }
        else if (line.matches(patternIsA.regex)) {
          val patternIsA(id, _, name) = line
          val headOnto = current.head
          val headOntoCopy = headOnto.copy(parents = headOnto.parents :+ OntologyTerm(id, name, Nil))
          headOntoCopy :: current.tail
        }
        else {
          current
        }
      }
      case Failure(_) => List.empty[OntologyTerm] //TODO Log Failure
    }
  }

  def addParentsToAncestors(map: Map[String, OntologyTerm]): Map[String, OntologyTerm] = {
    map.mapValues(v => v.copy(parents = addParents(v.parents, map)))
  }

  def addParents(seqOntologyTerm: Seq[OntologyTerm], map: Map[String, OntologyTerm]): Seq[OntologyTerm] = {
    seqOntologyTerm.map(t => t.copy(parents = map(t.id).parents))
  }

  def transformOntologyData(data: Map[String, OntologyTerm]) = {
    val allParents = data.values.flatMap(_.parents.map(_.id)).toSet
    data.flatMap(term => {
      val cumulativeList = mutable.Map.empty[OntologyTerm, Set[OntologyTerm]]
      getAllParentPath(term._2, term._2, data, Set.empty[OntologyTerm], cumulativeList, allParents)
    })
  }

  def getAllParentPath(term: OntologyTerm, originalTerm: OntologyTerm, data: Map[String, OntologyTerm], list: Set[OntologyTerm], cumulativeList: mutable.Map[OntologyTerm, Set[OntologyTerm]], allParents: Set[String]): mutable.Map[OntologyTerm, Set[OntologyTerm]] = {
    term.parents.foreach(p => {
      val parentTerm = data(p.id)

      if (parentTerm.parents.isEmpty) {
        cumulativeList.get(originalTerm) match {
          case Some(value) => cumulativeList.update(originalTerm, value ++ list + p)
          case None => cumulativeList.update(originalTerm, list + p)
        }
      }
      else {
        getAllParentPath(parentTerm, originalTerm, data, list + p, cumulativeList, allParents)
      }
    })
    cumulativeList
  }

  def readTextFileWithTry(url: String): Try[List[String]] = {
    Try {
      val lines = using(Source.fromURL(url)) { source =>
        (for (line <- source.getLines) yield line).toList
      }
      lines
    }
  }

  private def getICDsHeaderColumns(headerRowIterator: java.util.Iterator[Cell]): mutable.Map[String, Int] = {
    val mapColumns = scala.collection.mutable.Map[String, Int]()

    while(headerRowIterator.hasNext){
      val cell = headerRowIterator.next()
      val cellValue: String = cell.getStringCellValue
      cellValue match {
        case "8Y" => mapColumns("8Y") = cell.getColumnIndex
        case "Title" => mapColumns("Title") = cell.getColumnIndex
        case "ChapterNo" => mapColumns("ChapterNo") = cell.getColumnIndex
        case "isLeaf" => mapColumns("isLeaf") = cell.getColumnIndex
        case "noOfNonResidualChildren" => mapColumns("noOfNonResidualChildren") = cell.getColumnIndex
        case _  =>
      }
    }
    mapColumns
  }

  private def getICDsConversionHeaderColumns(headerRowIterator: java.util.Iterator[Cell]): mutable.Map[String, Int] = {
    val mapColumns = scala.collection.mutable.Map[String, Int]()

    for(cell <- headerRowIterator.asScala){
      if(cell.getCellType == CellType.STRING){
        cell.getStringCellValue match {
          case "icd11Code" => mapColumns("icd11Code") = cell.getColumnIndex
          case "icd11Chapter" => mapColumns("icd11Chapter") = cell.getColumnIndex
          case "icd11Title" => mapColumns("icd11Title") = cell.getColumnIndex
          case "icd10Code" => mapColumns("icd10Code") = cell.getColumnIndex
          case "icd10Chapter" => mapColumns("icd10Chapter") = cell.getColumnIndex
          case "icd10Title" => mapColumns("icd10Title") = cell.getColumnIndex
          case _  =>
        }
      }
    }

    mapColumns
  }

  private def getRowIterator(inputUrl: String) = {
    val f = new File(inputUrl)
    val workbook = WorkbookFactory.create(f)
    val sheet = workbook.getSheetAt(0)
    sheet.iterator()
  }

  def downloadICDs(inputFileUrl: String): List[ICDTerm] = {
    val rowIterator = getRowIterator(inputFileUrl)

    val headerCols = getICDsHeaderColumns(rowIterator.next().cellIterator())

    val icdTerms = mutable.MutableList[ICDTerm]()
    val parents = mutable.Stack[ICDTerm]()

    val pattern = """^([- ]*)(.+)""".r

    var currentLevel = 0
    var currentParentTitle = ""
    var rowLevel = 0
    var rowTitle = ""

    while(rowIterator.hasNext){

      val row = rowIterator.next()

      val eightY = Option(row.getCell(headerCols("8Y"))) match {
        case Some(v) => Some(v.getStringCellValue)
        case _ => None
      }
      val chapterNumber = row.getCell(headerCols("ChapterNo")).getStringCellValue
      val is_leaf = row.getCell(headerCols("isLeaf")).getStringCellValue.toBoolean
      val roughTitle = row.getCell(headerCols("Title")).getStringCellValue

      pattern.findAllIn(roughTitle).matchData foreach {
        m => {
          rowLevel = m.group(1).count(_ == '-')
          rowTitle = m.group(2)
        }
      }

      if(rowLevel == 0) currentParentTitle = rowTitle

      val levelDelta = currentLevel - rowLevel
      levelDelta match {
        case _ if levelDelta < 0 =>
          currentLevel = rowLevel
          parents.push(ICDTerm(title = currentParentTitle, eightY = eightY))
          currentParentTitle = rowTitle

        case _ if levelDelta > 0 =>
          currentParentTitle = rowTitle
          for(_ <- 1 to levelDelta) {
            currentLevel -= 1
            if(!(currentLevel < 0) ){
              parents.pop()
            }
          }

        case _ => currentParentTitle = rowTitle
      }

      val icd = ICDTerm(
        eightY = eightY,
        title = rowTitle,
        chapterNumber = chapterNumber,
        is_leaf = is_leaf,
        parents = parents.clone()
      )
      icdTerms += icd
    }
    icdTerms.toList
  }

  def transformIcd11To10(icds: List[ICDTerm], inputFileUrl: String): List[ICDTerm] = {
    val rowIterator = getRowIterator(inputFileUrl)

    val headerCols = getICDsConversionHeaderColumns(rowIterator.next().cellIterator())

    val conversionIterator = rowIterator.asScala.flatMap(r => {
      r.getCell(headerCols("icd10Code")).getStringCellValue.trim match {
        case "No Mapping" => None
        case _ => Some(ICDTermConversion(
          fromCode = getOptionalCellValue(r, headerCols("icd11Code")),
          toCode = getOptionalCellValue(r, headerCols("icd10Code")),
          fromChapter = r.getCell(headerCols("icd11Chapter")).getStringCellValue,
          toChapter = r.getCell(headerCols("icd10Chapter")).getStringCellValue,
          fromTitle = r.getCell(headerCols("icd11Title")).getStringCellValue,
          toTitle = r.getCell(headerCols("icd10Title")).getStringCellValue
        ))
      }
    })

    val conversions = conversionIterator.toList

    val convertedICDs = icds.flatMap(icd => {
      convertICD(icd, conversions)
    }).toSet

    val cleanICDs = removeICDWTermsInParents(convertedICDs)
    cleanICDs
  }

  private def getOptionalCellValue(row: Row, colPosition: Int) = {
    Option(row.getCell(colPosition)) match {
      case Some(cell) => Some(cell.getStringCellValue)
      case None => None
    }
  }

  private def convertICD(icd: ICDTerm, conversionDictionary: List[ICDTermConversion]): Option[ICDTerm] = {
    val icdTermConversion = conversionDictionary.find(t => t.fromTitle.trim == icd.title.trim)

    icdTermConversion match {
      case Some(t) => Some(ICDTerm(
        eightY = t.toCode,
        title = t.toTitle,
        chapterNumber = t.toChapter,
        is_leaf = icd.is_leaf,
        parents = icd.parents.flatMap(i => convertICD(i, conversionDictionary))
      ))
      case None => None
    }
  }

  //Due to downgrade conversion, remove ICD terms that have the current term in his parents
  //ex. term A.3 -- parents: [A.3, A.2, A.1]
  private def removeICDWTermsInParents(icds: Set[ICDTerm]): List[ICDTerm] = {
    val groupByTitle = icds.groupBy(_.title)
    groupByTitle.map(r => r._2.filterNot(t => t.parents.exists(i => i.title == t.title)).head).toList
  }
}
