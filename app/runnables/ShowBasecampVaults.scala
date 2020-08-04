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

class ShowBasecampVaults @Inject()(
  basecamp3Service: Basecamp3Service,
  configuration: Configuration
) extends InputFutureRunnableExt[ShowBasecampVaultsSpec] with RunnableHtmlOutputExt {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val execContext = materializer.executionContext

  private val accountId = configuration.getInt("basecamp3.account_id").getOrElse(
    throw new AdaException("The property 'basecamp3.account_id' not defined in custom.conf.")
  )

  override def runAsFuture(input: ShowBasecampVaultsSpec) =
      for  {
        vaults <- basecamp3Service.vaults(
          accountId,
          input.bucket,
          input.vault
        )
      } yield
        vaults.foreach(vault =>
          addParagraph(bold(vault.title) + " -> " + vault.id + " (# uploads: " + vault.uploads_count + ")")
        )
}

case class ShowBasecampVaultsSpec(
  bucket: Long,
  vault: Long
)
