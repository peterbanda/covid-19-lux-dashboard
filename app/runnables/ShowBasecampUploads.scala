package runnables

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.bnd.basecampclient.service.Basecamp3Service
import javax.inject.Inject
import org.ada.server.AdaException
import org.ada.web.services.RunnableHtmlOutputExt
import org.incal.core.runnables.InputFutureRunnableExt
import play.api.Configuration

class ShowBasecampUploads @Inject()(
  basecamp3Service: Basecamp3Service,
  configuration: Configuration
) extends InputFutureRunnableExt[ShowBasecampUploadsSpec] with RunnableHtmlOutputExt {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val execContext = materializer.executionContext

  private val accountId = configuration.getInt("basecamp3.account_id").getOrElse(
    throw new AdaException("The property 'basecamp3.account_id' not defined in custom.conf.")
  )

  override def runAsFuture(input: ShowBasecampUploadsSpec) =
      for  {
        uploads <- basecamp3Service.uploads(
          accountId,
          input.bucket,
          input.vault
        )
      } yield
        uploads.foreach(upload =>
          addParagraph(bold(upload.filename) + " -> " + upload.id + ", " + upload.download_url)
        )
}

case class ShowBasecampUploadsSpec(
  bucket: Long,
  vault: Long
)
