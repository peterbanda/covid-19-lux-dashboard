package runnables

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.bnd.basecampclient.service.Basecamp3Service
import javax.inject.Inject
import org.ada.server.AdaException
import org.incal.core.runnables.InputFutureRunnableExt
import play.api.{Configuration, Logger}

import scala.concurrent.Future

class DownloadBasecampFilesFromVault @Inject()(
  basecamp3Service: Basecamp3Service,
  configuration: Configuration
) extends InputFutureRunnableExt[DownloadBasecampFilesFromVaultSpec] {

  private val logger = Logger
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val execContext = materializer.executionContext

  private val accountId = configuration.getInt("basecamp3.account_id").getOrElse(
    throw new AdaException("The property 'basecamp3.account_id' not defined in custom.conf.")
  )
  private val downloadPath = configuration.getString("basecamp3.download.path").getOrElse(
    throw new AdaException("The property 'basecamp3.download.path' not defined in custom.conf.")
  )

  override def runAsFuture(input: DownloadBasecampFilesFromVaultSpec) =
    for  {
      uploads <- basecamp3Service.uploads(
        accountId,
        input.bucket,
        input.vault
      )

      _ <- Future.sequence(
        uploads.map { upload =>
          val folder = input.folder.map(_ + "/").getOrElse("")
          downloadFile(input.bucket, upload.id, upload.filename, folder + upload.parent.title, input.overwrite)
        }
      )
    } yield
      ()

  private def downloadFile(
    bucket: Long,
    upload: Long,
    fileName: String,
    folderName: String,
    overwrite: Boolean
  ) = {
    val path = (downloadPath + folderName).replaceAllLiterally(" ", "_")
    val filePath = path + "/" + fileName

    if (overwrite || !new File(filePath).exists) {
      for {
        fileSource <- basecamp3Service.downloadFileStreamed(
          accountId,
          bucket,
          upload,
          fileName
        )

        _ <- {
          logger.info(s"Downloading a file '$fileName' from Basecamp to '$filePath'.")
          val folder = new File(path)
          createFolderIfNeededNested(folder)

          fileSource.runWith(FileIO.toPath(Paths.get(filePath)))
        }
      } yield
        ()
    } else {
      logger.info(s"Skipping a file '$fileName' from Basecamp. Already exists at '$filePath'.")
      Future(())
    }
  }

  private def createFolderIfNeededNested(folder: File): Unit =
    if (!folder.exists) {
      if (folder.getParentFile != null) {
        createFolderIfNeededNested(folder.getParentFile)
      }
      folder.mkdir
    }
}

case class DownloadBasecampFilesFromVaultSpec(
  bucket: Long,
  vault: Long,
  folder: Option[String],
  overwrite: Boolean = false
)
