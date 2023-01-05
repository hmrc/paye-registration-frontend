/*
 * Copyright 2023 HM Revenue & Customs
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

import com.codahale.metrics.Timer
import config.AppConfig
import connectors.httpParsers.IncorporationInformationHttpParsers
import enums.IncorporationStatus
import models.external.{CoHoCompanyDetailsModel, IncorpUpdateResponse, OfficerList}
import play.api.libs.json._
import play.api.mvc.Request
import services.MetricsService
import uk.gov.hmrc.http._
import utils.RegistrationAllowlist

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait IncorpInfoResponse
case class IncorpInfoSuccessResponse(response: CoHoCompanyDetailsModel) extends IncorpInfoResponse
case object IncorpInfoBadRequestResponse extends IncorpInfoResponse
case object IncorpInfoNotFoundResponse extends IncorpInfoResponse
case class IncorpInfoErrorResponse(ex: Exception) extends IncorpInfoResponse

class IncorporationInformationConnector @Inject()(val metricsService: MetricsService,
                                                  val http: HttpClient
                                                 )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends BaseConnector with RegistrationAllowlist with IncorporationInformationHttpParsers {

  val incorpInfoUrl = appConfig.servicesConfig.baseUrl("incorporation-information")
  val incorpInfoUri = appConfig.servicesConfig.getString("microservice.services.incorporation-information.uri")
  val payeRegFeUrl = appConfig.servicesConfig.getString("microservice.services.paye-registration-frontend.ii-callback.url")
  val successCounter = metricsService.companyDetailsSuccessResponseCounter
  val failedCounter = metricsService.companyDetailsFailedResponseCounter

  def timer: Timer.Context = metricsService.incorpInfoResponseTimer.time()

  def setupSubscription(transactionId: String, regId: String, regime: String = "paye-fe", subscriber: String = "SCRS")
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[Option[IncorporationStatus.Value]] = {
    withRecovery()("setupSubscription", Some(regId), Some(transactionId)) {
      implicit val reads = IncorpUpdateResponse.reads(transactionId, subscriber, regime)
      http.POST[JsObject, Option[IncorporationStatus.Value]](
        url = s"$incorpInfoUrl${s"/incorporation-information/subscribe/$transactionId/regime/$regime/subscriber/$subscriber"}",
        body = Json.obj("SCRSIncorpSubscription" -> Json.obj("callbackUrl" -> s"$payeRegFeUrl/company-incorporation"))
      )(implicitly, optionHttpReads("setupSubscription", Some(regId), Some(transactionId)), hc, ec)
    }
  }

  def cancelSubscription(transactionId: String, regId: String, regime: String = "paye-fe", subscriber: String = "SCRS")
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] =
    withRecovery(response = Some(false))("cancelSubscription", Some(regId), Some(transactionId)) {
      http.DELETE[Boolean](
        url = s"$incorpInfoUrl/incorporation-information/subscribe/$transactionId/regime/$regime/subscriber/$subscriber"
      )(cancelSubscriptionHttpReads(regId, transactionId), hc, ec)
    }

  def getCoHoCompanyDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[IncorpInfoResponse] =
    ifRegIdNotAllowlisted(regId) {
      val incorpInfoTimer: Timer.Context = timer
      http.GET[IncorpInfoResponse](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile")(getCoHoCompanyDetailsHttpReads(regId, transactionId), hc, ec).map { response =>
        incorpInfoTimer.stop()
        if(response.isInstanceOf[IncorpInfoSuccessResponse]) successCounter.inc() else failedCounter.inc()
        response
      } recover {
        case ex: Exception =>
          logger.error(s"[getCoHoCompanyDetails] Received an error when expecting company details for regId: $regId / TX-ID: $transactionId error: ${ex.getMessage}")
          incorpInfoTimer.stop()
          failedCounter.inc()
          IncorpInfoErrorResponse(ex)
      }
    }

  def getIncorporationInfoDate(regId: String, txId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[LocalDate]] =
    withTimer {
      withRecovery()("getIncorporationInfoDate", Some(regId), Some(txId)) {
        http.GET[Option[LocalDate]](s"$incorpInfoUrl$incorpInfoUri/$txId/incorporation-update")(getIncorpInfoDateHttpReads(regId, txId), hc, ec)
      }
    }

  def getOfficerList(transactionId: String, regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[OfficerList] =
    ifRegIdNotAllowlisted(regId) {
      withTimer {
        withRecovery()("getOfficerList", Some(regId), Some(transactionId)) {
          http.GET[OfficerList](s"$incorpInfoUrl$incorpInfoUri/$transactionId/officer-list")(
            getOfficersHttpReads(regId, transactionId), hc, ec
          )
        }
      }
    }

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics(metricsService.incorpInfoResponseTimer.time())(f)
}