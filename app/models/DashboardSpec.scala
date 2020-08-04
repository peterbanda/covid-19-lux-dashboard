package models

import java.util.Date

import org.ada.server.dataaccess.BSONObjectIdentity
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

case class DashboardSpec(
  _id: Option[BSONObjectID] = None,
  sourceDataSetIds: Seq[String],
  timeCreated: Date = new Date(),
  timeLastUpdated: Option[Date] = None
)

object DashboardSpec {
  implicit val dashboardSpecformat = Json.format[DashboardSpec]

  implicit object DashboardSpecIdentity extends BSONObjectIdentity[DashboardSpec] {
    def of(entity: DashboardSpec): Option[BSONObjectID] = entity._id
    protected def set(entity: DashboardSpec, id: Option[BSONObjectID]) = entity.copy(_id = id)
  }
}