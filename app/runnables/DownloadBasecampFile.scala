package runnables

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.bnd.basecampclient.service.Basecamp3Service
import javax.inject.Inject
import org.ada.server.AdaException
import org.incal.core.runnables.InputFutureRunnableExt
import play.api.Configuration

class DownloadBasecampFile @Inject()(
  basecamp3Service: Basecamp3Service,
  configuration: Configuration
) extends InputFutureRunnableExt[DownloadBasecampFileSpec] {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val execContext = materializer.executionContext

  private val accountId = configuration.getInt("basecamp3.account_id").getOrElse(
    throw new AdaException("The property 'basecamp3.account_id' not defined in custom.conf.")
  )
  private val downloadPath = configuration.getString("basecamp3.download.path") match {
    case Some(path) => path
    case None =>
      configuration.getString("datasetimport.import.folder").getOrElse(
        throw new AdaException("The property 'basecamp3.download.path' or 'datasetimport.import.folder' not defined in custom.conf.")
      )
  }

  private val pathWithBackslash = if (downloadPath.endsWith("/")) downloadPath else downloadPath + "/"

  override def runAsFuture(input: DownloadBasecampFileSpec) =
    for  {
      fileSource <- basecamp3Service.downloadFileStreamed(
        accountId,
        input.bucket,
        input.upload,
        input.fileName
      )

      _ <- fileSource.runWith(FileIO.toPath(Paths.get(pathWithBackslash + input.fileName)))
    } yield
        ()
}

case class DownloadBasecampFileSpec(
  bucket: Long,
  upload: Long,
  fileName: String
)
