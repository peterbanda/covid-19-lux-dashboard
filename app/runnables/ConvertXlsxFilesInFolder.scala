package runnables

import javax.inject.Inject
import org.incal.core.runnables.{InputRunnable, InputRunnableExt}
import org.incal.core.util.listFiles
import play.api.Logger

class ConvertXlsxFilesInFolder @Inject() (
  convertXlsxToCsv: ConvertXlsxToCsv
) extends InputRunnableExt[ConvertXlsxFilesInFolderSpec] {

  private val logger = Logger
  private val ext = "xlsx"

  override def run(input: ConvertXlsxFilesInFolderSpec) =
    listFiles(input.folderName).filter(_.getName.endsWith(ext)).map { xlsxFile =>
      try {
        convertXlsxToCsv.run(ConvertXlsxToCsvSpec(
          fileName = xlsxFile.getAbsolutePath,
          input.delimiter,
          input.overwrite
        ))
      } catch {
        case e: Exception =>
          logger.error(s"Converting of an ${ext} file '${xlsxFile.getAbsolutePath}' failed.'", e)
      }
    }
}

case class ConvertXlsxFilesInFolderSpec(
  folderName: String,
  delimiter: Option[String] = None,
  overwrite: Boolean = true
)