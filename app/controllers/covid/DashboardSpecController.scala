package controllers.covid

import java.util.Date

import javax.inject.Inject
import models.DashboardSpec
import org.ada.server.dataaccess.RepoTypes._
import models.DashboardSpec
import models.DashboardSpec._
import org.ada.web.controllers.core.AdaCrudControllerImpl
import org.incal.play.controllers.{AdminRestrictedCrudController, HasBasicFormCrudViews, HasFormShowEqualEditView}
import play.api.data.Form
import play.api.data.Forms.{date, default, ignored, mapping, nonEmptyText, optional, seq}
import reactivemongo.bson.BSONObjectID
import services.BatchOrderRequestRepoTypes.DashboardSpecRepo
import views.html.{dashboardSpec => view}

class DashboardSpecController @Inject() (
    dashboardSpecRepo: DashboardSpecRepo
  ) extends AdaCrudControllerImpl[DashboardSpec, BSONObjectID](dashboardSpecRepo)
    with AdminRestrictedCrudController[BSONObjectID]
    with HasBasicFormCrudViews[DashboardSpec, BSONObjectID]
    with HasFormShowEqualEditView[DashboardSpec, BSONObjectID] {

  override protected[controllers] val form = Form(
    mapping(
      "_id" -> ignored(Option.empty[BSONObjectID]),
      "sourceDataSetIds" -> seq(nonEmptyText),
      "timeCreated" -> default(date("yyyy-MM-dd HH:mm:ss"), new Date()),
      "timeLastUpdated" -> optional(date("yyyy-MM-dd HH:mm:ss"))
    )(DashboardSpec.apply)(DashboardSpec.unapply))

  override protected val homeCall = controllers.covid.routes.DashboardSpecController.find()
  override protected def createView = { implicit ctx => view.create(_) }
  override protected def editView = { implicit ctx => view.edit(_) }
  override protected def listView = { implicit ctx => (view.list(_, _)).tupled }
}