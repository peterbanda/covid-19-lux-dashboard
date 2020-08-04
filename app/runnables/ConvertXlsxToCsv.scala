package runnables

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.{CellStyle, CellType, Row}
import java.io.{File, FileInputStream}
import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import javax.inject.Inject
import org.ada.server.field.FieldTypeHelper
import org.incal.core.akka.AkkaFileIO.writeLines
import org.incal.core.runnables.{InputFutureRunnableExt, InputRunnableExt}
import org.incal.core.util.ReflectionUtil
import play.api.{Configuration, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.Future

class ConvertXlsxToCsv extends InputRunnableExt[ConvertXlsxToCsvSpec] {

  private val defaultDelimiter = ","
  private val logger = Logger

  private val dateFormats = Set(
    "m/d/yy",
    "dd/mm/yyyy",
    "dd\\/mm\\/yyyy",
    "dd\\/mm\\/yyyy\\ hh:mm",
    "yyyy-mm-dd HH:MM:SS",
    "yyyy\\-mm\\-dd\\ hh:mm:ss",
    "d-mmm-yy",
    "d-mmm",
    "mmm-yy",
    "m/d/yy h:mm"
  )

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val execContext = system.dispatcher

  override def run(input: ConvertXlsxToCsvSpec) = {
    val outputFile = outputFileName(input.fileName)

    if (input.overwrite || !new File(outputFile).exists) {

      val excelFile = new FileInputStream(input.fileName)
      val wb = new XSSFWorkbook(excelFile)
      val sheet = wb.getSheetAt(0)
      val rows = sheet.rowIterator

      val delimiter = input.delimiter.getOrElse(defaultDelimiter)

      val csvRows = rows.map { row =>
        val values = for (i <- 0 until row.getLastCellNum()) yield {
          readSafe(row, i).getOrElse("")
        }

        values.mkString(delimiter)
      }

      logger.info(s"Converting an xlsx file '${input.fileName}' to csv.")

      writeLines(Source.fromIterator(() => csvRows), outputFile)
    } else {
      logger.info(s"Skipping an xlsx file '${input.fileName}' for csv conversion. Target file already exists.")
    }
  }

  private def outputFileName(fileName: String) = {
    val lastDot = fileName.lastIndexOf(".")
    val baseName = if (lastDot > -1) fileName.take(lastDot) else fileName

    baseName + ".csv"
  }

  private def readSafe(row: Row, cellIndex: Int) =
    Option(row.getCell(cellIndex)).flatMap { cell =>
      cell.getCellType match {
        case CellType.STRING =>
          Some("\"" + cell.getStringCellValue + "\"")

        case CellType.NUMERIC =>
          if (dateFormats.contains(cell.getCellStyle.getDataFormatString)) {
            val dateFormat = new SimpleDateFormat(FieldTypeHelper.displayDateFormat)
            Some(dateFormat.format(cell.getDateCellValue))
          } else
            Some(cell.getNumericCellValue.toString)

        case CellType.BOOLEAN =>
          Some(cell.getBooleanCellValue.toString)

        case _ => None
      }
    }
}

case class ConvertXlsxToCsvSpec(
  fileName: String,
  delimiter: Option[String] = None,
  overwrite: Boolean = true
)