package services

import models.DashboardSpec
import net.codingwell.scalaguice.ScalaModule
import org.ada.server.dataaccess.mongo.MongoAsyncCrudRepo
import org.incal.core.dataaccess.AsyncCrudRepo
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats._
import services.BatchOrderRequestRepoTypes.DashboardSpecRepo
import models.DashboardSpec._

class DashboardModule extends ScalaModule {

  override def configure = {
    bind[DashboardSpecRepo].toInstance(
      new MongoAsyncCrudRepo[DashboardSpec, BSONObjectID]("dashboard_specs")
    )
  }
}

object BatchOrderRequestRepoTypes {
  type DashboardSpecRepo = AsyncCrudRepo[DashboardSpec, BSONObjectID]
}
