package runnables

import java.io.File

import javax.inject.Inject
import org.incal.core.runnables.InputRunnableExt

class ConvertXlsxFilesInFolderRecursively @Inject() (
  convertXlsxFilesInFolder: ConvertXlsxFilesInFolder
) extends InputRunnableExt[ConvertXlsxFilesInFolderRecursivelySpec] {

  override def run(input: ConvertXlsxFilesInFolderRecursivelySpec) =
    convertRecursively(
      input.folderName,
      input.delimiter,
      input.overwrite
    )

  private def convertRecursively(
    folderName: String,
    delimiter: Option[String],
    overwrite: Boolean
  ): Unit = {
    convertXlsxFilesInFolder.run(ConvertXlsxFilesInFolderSpec(
      folderName,
      delimiter,
      overwrite
    ))

    listDirs(folderName).foreach { folder =>
      convertRecursively(folder.getAbsolutePath, delimiter, overwrite)
    }
  }

  private def listDirs(dir: String): Seq[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory)
      d.listFiles.filter(_.isDirectory).toList
    else
      Nil
  }
}

case class ConvertXlsxFilesInFolderRecursivelySpec(
  folderName: String,
  delimiter: Option[String] = None,
  overwrite: Boolean = true
)
