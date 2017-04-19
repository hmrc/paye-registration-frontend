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
import models.external.{CHROAddress, CoHoCompanyDetailsModel, OfficerList}
import play.api.Logger
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncorporationInformationConnector @Inject()(injMetrics: MetricsService) extends IncorporationInformationConnect with ServicesConfig {
  lazy val coHoAPIUrl = baseUrl("coho-api")
  lazy val coHoAPIUri = getConfString("coho-api.uri","")
  lazy val incorpInfoUrl = baseUrl("incorporation-information")
  lazy val incorpInfoUri = getConfString("incorporation-information.uri","")
  val http : WSHttp = WSHttp
  val metricsService = injMetrics
}

sealed trait IncorpInfoResponse
case class IncorpInfoSuccessResponse(response: CoHoCompanyDetailsModel) extends IncorpInfoResponse
case object IncorpInfoBadRequestResponse extends IncorpInfoResponse
case class IncorpInfoErrorResponse(ex: Exception) extends IncorpInfoResponse
case class IncorpInfoROAddress(response : CHROAddress) extends IncorpInfoResponse

trait IncorporationInformationConnect {

  val coHoAPIUrl: String
  val coHoAPIUri: String
  val incorpInfoUrl: String
  val incorpInfoUri: String
  val http: WSHttp
  val metricsService: MetricsSrv

  def getCoHoCompanyDetails(registrationID: String)(implicit hc: HeaderCarrier): Future[IncorpInfoResponse] = {
    val cohoApiTimer = metricsService.cohoAPIResponseTimer.time()
    http.GET[CoHoCompanyDetailsModel](s"$coHoAPIUrl$coHoAPIUri/company/$registrationID") map { res =>
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

  def getRegisteredOfficeAddress(transactionId: String)(implicit hc : HeaderCarrier): Future[CHROAddress] = {
    val cohoApiTimer = metricsService.cohoAPIResponseTimer.time()
    http.GET[CHROAddress](s"$coHoAPIUrl$coHoAPIUri/$transactionId/ro-address") map { roAddress =>
      cohoApiTimer.stop()
      roAddress
    } recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CohoAPIConnector] [getRegisteredOfficeAddress] - Received a BadRequest status code when expecting a Registered office address")
        cohoApiTimer.stop()
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CohoAPIConnector] [getRegisteredOfficeAddress] - Received an error response when expecting a Registered office address - error: ${ex.getMessage}")
        cohoApiTimer.stop()
        throw ex
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
