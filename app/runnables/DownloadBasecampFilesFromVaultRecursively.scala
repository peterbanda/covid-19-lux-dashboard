package runnables

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bnd.basecampclient.model.Vault
import com.bnd.basecampclient.service.Basecamp3Service
import javax.inject.Inject
import org.ada.server.AdaException
import org.incal.core.runnables.InputFutureRunnableExt
import play.api.{Configuration, Logger}
import org.incal.core.util.seqFutures

import scala.concurrent.Future

class DownloadBasecampFilesFromVaultRecursively @Inject()(
  basecamp3Service: Basecamp3Service,
  configuration: Configuration,
  downloadBasecampFilesFromVault: DownloadBasecampFilesFromVault
) extends InputFutureRunnableExt[DownloadBasecampFilesFromVaultRecursivelySpec] {

  private val logger = Logger
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val execContext = materializer.executionContext

  private val accountId = configuration.getInt("basecamp3.account_id").getOrElse(
    throw new AdaException("The property 'basecamp3.account_id' not defined in custom.conf.")
  )

  override def runAsFuture(input: DownloadBasecampFilesFromVaultRecursivelySpec) =
    downloadRecursively(input.bucket, input.vault, input.overwrite, input.folderName)

  private def downloadRecursively(
    bucket: Long,
    vault: Long,
    overwrite: Boolean,
    baseFolder: Option[String]
  ): Future[Unit] =
    for  {
      vaults <- basecamp3Service.vaults(
        accountId,
        bucket,
        vault
      )

      _ <- downloadBasecampFilesFromVault.runAsFuture(
        DownloadBasecampFilesFromVaultSpec(
          bucket,
          vault,
          baseFolder,
          overwrite
        )
      )

      _ <- seqFutures(vaults) { vault =>
        val parentTitle = vault.parent.title
        val folder = baseFolder.map(_ + "/").getOrElse("")
        downloadRecursively(bucket, vault.id, overwrite, Some(folder + parentTitle))
      }
    } yield
      ()
}

case class DownloadBasecampFilesFromVaultRecursivelySpec(
  bucket: Long,
  vault: Long,
  folderName: Option[String] = None,
  overwrite: Boolean = false
)
