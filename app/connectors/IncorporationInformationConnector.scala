/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.WSHttp
import models.external.{CoHoCompanyDetailsModel, OfficerList}
import play.api.Logger
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp
import utils.RegistrationWhitelist

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncorporationInformationConnector @Inject()(injMetrics: MetricsService) extends IncorporationInformationConnect with ServicesConfig {
  lazy val incorpInfoUrl = baseUrl("incorporation-information")
  lazy val incorpInfoUri = getConfString("incorporation-information.uri","")
  val http : WSHttp = WSHttp
  val metricsService = injMetrics
}

sealed trait IncorpInfoResponse
case class IncorpInfoSuccessResponse(response: CoHoCompanyDetailsModel) extends IncorpInfoResponse
case object IncorpInfoBadRequestResponse extends IncorpInfoResponse
case class IncorpInfoErrorResponse(ex: Exception) extends IncorpInfoResponse

trait IncorporationInformationConnect extends RegistrationWhitelist {

  val incorpInfoUrl: String
  val incorpInfoUri: String
  val http: WSHttp
  val metricsService: MetricsSrv

  def getCoHoCompanyDetails(regId: String, transactionId: String)(implicit hc: HeaderCarrier): Future[IncorpInfoResponse] = {
    ifRegIdNotWhitelisted(regId) {
      implicit val rds = CoHoCompanyDetailsModel.incorpInfoReads
      val cohoApiTimer = metricsService.cohoAPIResponseTimer.time()
      http.GET[CoHoCompanyDetailsModel](s"$incorpInfoUrl$incorpInfoUri/$transactionId/company-profile") map { res =>
        cohoApiTimer.stop()
        IncorpInfoSuccessResponse(res)
      } recover {
        case badRequestErr: BadRequestException =>
          Logger.error("[CohoAPIConnector] [getCoHoCompanyDetails] - Received a BadRequest status code when expecting company details")
          cohoApiTimer.stop()
          IncorpInfoBadRequestResponse
        case ex: Exception =>
          Logger.error(s"[CohoAPIConnector] [getIncorporationStatus] - Received an error response when expecting company details - error: ${ex.getMessage}")
          cohoApiTimer.stop()
          IncorpInfoErrorResponse(ex)
      }
    }
  }

  def getOfficerList(transactionId: String)(implicit hc : HeaderCarrier): Future[OfficerList] = {
    val cohoApiTimer = metricsService.cohoAPIResponseTimer.time()
    http.GET[OfficerList](s"$incorpInfoUrl$incorpInfoUri/$transactionId/officer-list") map { list =>
      cohoApiTimer.stop()
      list
    } recover {
      case e: NotFoundException =>
        cohoApiTimer.stop()
        OfficerList(items = Nil)
      case badRequestErr: BadRequestException =>
        Logger.error("[CohoAPIConnector] [getOfficerList] - Received a BadRequest status code when expecting an Officer list")
        cohoApiTimer.stop()
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CohoAPIConnector] [getOfficerList] - Received an error response when expecting an Officer list - error: ${ex.getMessage}")
        cohoApiTimer.stop()
        throw ex
    }
  }
}
