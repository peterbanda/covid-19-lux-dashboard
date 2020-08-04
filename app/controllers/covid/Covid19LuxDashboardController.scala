package controllers.covid

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.inject.Inject
import org.ada.server.AdaException
import org.ada.server.calc.CalculatorExecutors
import org.ada.server.calc.CalculatorHelper._
import org.ada.server.dataaccess.RepoTypes.{FieldRepo, FilterRepo}
import org.ada.server.dataaccess.dataset.DataSetAccessorFactory
import org.ada.server.models.DataSetFormattersAndIds.FieldIdentity
import org.ada.web.controllers.core.AdaBaseController
import org.incal.core.dataaccess.{AscSort, Criterion, DescSort}
import org.incal.core.dataaccess.Criterion._
import views.html.iPortfolio.{index => indexView}
import org.ada.server.field.FieldUtil.{FieldOps, JsonFieldOps}
import org.ada.server.models.{DataView, Field, FieldTypeId, Filter, WidgetGenerationMethod, WidgetSpec}
import org.ada.web.services.WidgetGenerationService
import org.incal.play.security.AuthAction
import services.BatchOrderRequestRepoTypes.DashboardSpecRepo
import org.ada.server.dataaccess.dataset.FilterRepoExtra._
import org.ada.server.field.FieldUtil._
import org.incal.core.FilterCondition
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Covid19LuxDashboardController @Inject() (
  dsaf: DataSetAccessorFactory,
  dashboardSpecRepo: DashboardSpecRepo,
  widgetGenerationService: WidgetGenerationService
) extends AdaBaseController with CalculatorExecutors {

  private val caseAndHospitalDataSetId = "covid_19_lux.case_and_hospital_data"
  private val newCasesField = Field("new_cases", None, FieldTypeId.Integer)

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  def index =  AuthAction { implicit request =>
    // last items
    val getTestPerformedFuture = getLastItem[Long](
      caseAndHospitalDataSetId, "report_date", "tests_done"
    )

    val getRecoveredFuture = getLastItem[Double](
      caseAndHospitalDataSetId, "report_date", "people_healed"
    )

    val getDeathsFuture = getLastItem[Long](
      caseAndHospitalDataSetId, "report_date", "deaths"
    )

    for {
      dashboardSpec <- dashboardSpecRepo.find().map(_.headOption)

      widgets <- Future.sequence {
        val dataSetIds = dashboardSpec.map(_.sourceDataSetIds).getOrElse(Nil)

        dataSetIds.map { dataSetId =>
          val dsa = dsaSafe(dataSetId)

          for {
            views <- dsa.dataViewRepo.find()

            widgets <- Future.sequence(
              views.map(view => genWidgets(dataSetId, view))
            )
          } yield {
            widgets.flatten
          }
        }
      }

      newCasesBasicStats <- basicStatsExec.execJsonRepoStreamed_(
        withProjection = true,
        fields = newCasesField)(
        dataRepo = dsaSafe(caseAndHospitalDataSetId).dataSetRepo,
        criteria = Nil
      )

      testPerformed <- getTestPerformedFuture
      recovered <- getRecoveredFuture
      deaths <- getDeathsFuture
    } yield {
      val cases = newCasesBasicStats.map(_.sum.toInt).getOrElse(0)

      Ok(indexView(
        cases,
        deaths.map(_.toInt).getOrElse(0),
        recovered.map(_.toInt).getOrElse(0),
        testPerformed.map(_.toInt).getOrElse(0),
        widgets.flatten.flatten
      ))
    }
  }

  private def genWidgets(
    dataSetId: String,
    dataView: DataView
  ) =
    dsaf(dataSetId).map { dsa =>

      val toCriteria = toDataSetCriteria(dsa.fieldRepo, _: Seq[FilterCondition])

      for {
        fields <- {
          val fieldNames = dataView.widgetSpecs.flatMap(_.fieldNames)
          dsa.fieldRepo.find(Seq(FieldIdentity.name #-> fieldNames))
        }

        // if there are multiple filters associated with this view take only the first one
        resolvedFilter <- dataView.filterOrIds.headOption.map(dsa.filterRepo.resolve).getOrElse(Future(Filter()))

        // convert the conditions to criteria
        criteria <- toCriteria(resolvedFilter.conditions)

        filterSubcriteriaSet <- filterSubCriteriaSet(dsa.filterRepo, dsa.fieldRepo, dataView.widgetSpecs)

        // generate widgets
        widgets <- widgetGenerationService(
          dataView.widgetSpecs, dsa.dataSetRepo, criteria, filterSubcriteriaSet.toMap, fields, genMethod = WidgetGenerationMethod.Auto
        )
      } yield
        widgets
    }.getOrElse(Future(Nil))

  private def filterSubCriteriaSet(
    filterRepo: FilterRepo,
    fieldRepo: FieldRepo,
    widgetSpecs: Traversable[WidgetSpec]
  )  = {
    val toCriteria = toDataSetCriteria(fieldRepo, _: Seq[FilterCondition])

    Future.sequence(
      widgetSpecs.map(_.subFilterId).flatten.toSet.map { subFilterId: BSONObjectID =>

        filterRepo.resolve(Right(subFilterId)).flatMap(resolvedFilter =>
          toCriteria(resolvedFilter.conditions).map(criteria =>
            (subFilterId, criteria)
          )
        )
      }
    )
  }

  private def dsaSafe(dataSetId: String) =
    dsaf(dataSetId).getOrElse(throw new AdaException(s"Data set '${dataSetId}' not found."))

  private def getLastItem[T](
    dataSetId: String,
    sortFieldName: String,
    valueFieldName: String
  ): Future[Option[T]] =
    dsaf(dataSetId).map { dsa =>
      for {
        last <- dsa.dataSetRepo.find(sort = Seq(DescSort(sortFieldName)), limit = Some(1)).map(_.head)
        field <- dsa.fieldRepo.get(valueFieldName)
      } yield
        field.flatMap(field =>
          last.toValue(field.toNamedType[T])
        )
    }.getOrElse(Future(None))
}