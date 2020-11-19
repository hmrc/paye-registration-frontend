/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import com.codahale.metrics.{Counter, Timer}
import common.exceptions.DownstreamExceptions.{IncorporationInformationResponseException, OfficerListNotFoundException}
import config.{AppConfig, WSHttp}
import controllers.exceptions.GeneralException
import enums.IncorporationStatus
import javax.inject.Inject
import models.external.{CoHoCompanyDetailsModel, IncorpUpdateResponse, OfficerList}
import play.api.Logger
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json._
import services.MetricsService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationAllowlist

import scala.concurrent.Future

class IncorporationInformationConnectorImpl @Inject()(val metricsService: MetricsService,
                                                      val http: WSHttp
                                                     )(implicit val appConfig: AppConfig) extends IncorporationInformationConnector {

  lazy val incorpInfoUrl = appConfig.servicesConfig.baseUrl("incorporation-information")
  lazy val incorpInfoUri = appConfig.servicesConfig.getConfString("incorporation-information.uri", "")
  lazy val payeRegFeUrl = appConfig.servicesConfig.getConfString("paye-registration-frontend.ii-callback.url",
    throw new IllegalArgumentException("[IncorporationInformationConnector] config value payeRegFeUrl cannot be found"))
  val successCounter = metricsService.companyDetailsSuccessResponseCounter
  val failedCounter = metricsService.companyDetailsFailedResponseCounter

  def timer: Timer.Context = metricsService.incorpInfoResponseTimer.time()

}

sealed trait IncorpInfoResponse

case class IncorpInfoSuccessResponse(response: CoHoCompanyDetailsModel) extends IncorpInfoResponse

case object IncorpInfoBadRequestResponse extends IncorpInfoResponse

case object IncorpInfoNotFoundResponse extends IncorpInfoResponse

case class IncorpInfoErrorResponse(ex: Exception) extends IncorpInfoResponse

trait IncorporationInformationConnector extends RegistrationAllowlist {
  implicit val appConfig: AppConfig
  val incorpInfoUrl: String
  val incorpInfoUri: String
  val payeRegFeUrl: String
  val http: CoreGet with CorePost with CoreDelete
  val metricsService: MetricsService

  val successCounter: Counter
  val failedCounter: Counter

  def timer: Timer.Context

  def setupSubscription(transactionId: String, regId: String, regime: String = "paye-fe", subscriber: String = "SCRS")(implicit hc: HeaderCarrier): Future[Option[IncorporationStatus.Value]] = {
    def constructIncorporationInfoUri(transactionId: String, regime: String, subscriber: String): String = {
      s"/incorporation-information/subscribe/$transactionId/regime/$regime/subscriber/$subscriber"
    }

    val postJson = Json.obj("SCRSIncorpSubscription" -> Json.obj("callbackUrl" -> s"$payeRegFeUrl/company-incorporation"))
    http.POST[JsObject, HttpResponse](s"$incorpInfoUrl${constructIncorporationInfoUri(transactionId, regime, subscriber)}", postJson) map { resp =>
      resp.status match {
        case OK => Some(resp.json.as[IncorporationStatus.Value](IncorpUpdateResponse.reads(transactionId, subscriber, regime)))
        case ACCEPTED => None
        case _ =>
          Logger.warn(s"[IncorporationInformationConnect] - [setupSubscription] returned a successful response but with an incorrect code of: ${resp.status} for regId: $regId and txId: $transactionId")
          throw new IncorporationInformationResponseException(s"Calling II on ${constructIncorporationInfoUri(transactionId, regime, subscriber)} returned a ${resp.status}")
      }
    } recover {
      case e =>
        Logger.warn(s"[IncorporationInformationConnect] - [setupSubscription] an unexpected error ${e.getMessage} occurred when calling II for regId: $regId txId: $transactionId")
        throw e
    }
  }

  def cancelSubscription(transactionId: String, regId: String, regime: String = "paye-fe", subscriber: String = "SCRS")(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.DELETE[HttpResponse](s"$incorpInfoUrl/incorporation-information/subscribe/$transactionId/regime/$regime/subscriber/$subscriber").map(_ => true)
      .recover {
        case _: NotFoundException => Logger.info(s"[IncorporationInformationConnect] - [cancelSubscription] no subscription found when trying to delete subscription. it might already have been deleted")
          true
        case e =>
          Logger.warn(s"[IncorporationInformationConnect] - [cancelSubscription] an unexpected error ${e.getMessage} occurred when calling II for regId: $regId txId: $transactionId")
          false
      }
  }

  def getCoHoCompanyDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[IncorpInfoResponse] = {
    ifRegIdNotAllowlisted(regId) {
      implicit val rds: Reads[CoHoCompanyDetailsModel] = CoHoCompanyDetailsModel.incorpInfoReads

      val incorpInfoTimer: Timer.Context = timer

      http.GET[CoHoCompanyDetailsModel](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile") map {
        res =>
          incorpInfoTimer.stop()
          successCounter.inc(1)
          IncorpInfoSuccessResponse(res)
      } recover {
        case _: BadRequestException =>
          logger.error(s"[IncorporationInformationConnector] [getCoHoCompanyDetails] - Received a BadRequest status code when expecting company details for regId: $regId / TX-ID: $transactionId")
          incorpInfoTimer.stop()
          failedCounter.inc(1)
          IncorpInfoBadRequestResponse
        case _: NotFoundException =>
          logger.error(s"[IncorporationInformationConnector] - [getCoHoCompanyDetails] - Received a NotFound status code when expecting company details for regId: $regId / TX-ID: $transactionId")
          incorpInfoTimer.stop()
          failedCounter.inc(1)
          IncorpInfoNotFoundResponse
        case ex: Exception =>
          logger.error(
            s"[IncorporationInformationConnector] [getCoHoCompanyDetails] - Received an error when expecting company details for regId: $regId / TX-ID: $transactionId - error: ${ex.getMessage}"
          )
          incorpInfoTimer.stop()
          failedCounter.inc(1)
          IncorpInfoErrorResponse(ex)
      }
    }
  }

  def getIncorporationInfo(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val incorpInfoTimer = metricsService.incorpInfoResponseTimer.time()
    http.GET[HttpResponse](s"$incorpInfoUrl$incorpInfoUri/$txId/incorporation-update") map { value =>
      incorpInfoTimer.stop()
      if (value.status == 204) Json.obj() else value.json
    }
  } recover {
    case e: Exception =>
      throw GeneralException(
        s"[IncorporationInformationConnector][getIncorporationInfo] an error occurred while getting the incorporation info for regId: $regId and txId: $txId - error: ${e.getMessage}"
      )
  }

  def getOfficerList(transactionId: String, regId: String)(implicit hc: HeaderCarrier): Future[OfficerList] = {
    ifRegIdNotAllowlisted(regId) {
      val incorpInfoTimer = metricsService.incorpInfoResponseTimer.time()
      http.GET[JsObject](s"$incorpInfoUrl$incorpInfoUri/$transactionId/officer-list") map { obj =>
        incorpInfoTimer.stop()
        val list = obj.\("officers").as[OfficerList]
        if (list.items.isEmpty) {
          logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received an empty Officer list for TX-ID $transactionId")
          throw new OfficerListNotFoundException
        } else {
          list
        }
      } recover {
        case _: NotFoundException =>
          logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received a NotFound status code when expecting an Officer list for TX-ID $transactionId")
          incorpInfoTimer.stop()
          throw new OfficerListNotFoundException
        case badRequestErr: BadRequestException =>
          logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received a BadRequest status code when expecting an Officer list for TX-ID $transactionId")
          incorpInfoTimer.stop()
          throw badRequestErr
        case ex: Exception =>
          logger.error(s"[IncorporationInformationConnector] [getOfficerList] - Received an error response when expecting an Officer list for TX-ID $transactionId - error: ${ex.getMessage}")
          incorpInfoTimer.stop()
          throw ex
      }
    }
  }
}