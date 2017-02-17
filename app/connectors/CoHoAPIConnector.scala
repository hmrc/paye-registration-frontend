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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CoHoAPIConnector @Inject()() extends CoHoAPIConnect with ServicesConfig {
  lazy val coHoAPIUrl = baseUrl("coho-api")
  lazy val coHoAPIUri = getConfString("coho-api.uri","")
  val http : WSHttp = WSHttp
}

sealed trait CohoApiResponse
case class CohoApiSuccessResponse(response: CoHoCompanyDetailsModel) extends CohoApiResponse
case object CohoApiBadRequestResponse extends CohoApiResponse
case class CohoApiErrorResponse(ex: Exception) extends CohoApiResponse
case class CohoApiROAddress(response : CHROAddress) extends CohoApiResponse

trait CoHoAPIConnect {

  val coHoAPIUrl: String
  val coHoAPIUri: String
  val http: WSHttp

  def getCoHoCompanyDetails(registrationID: String)(implicit hc: HeaderCarrier): Future[CohoApiResponse] = {
    http.GET[CoHoCompanyDetailsModel](s"$coHoAPIUrl$coHoAPIUri/company/$registrationID") map {
      res => CohoApiSuccessResponse(res)
    } recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CohoAPIConnector] [getCoHoCompanyDetails] - Received a BadRequest status code when expecting company details")
        CohoApiBadRequestResponse
      case ex: Exception =>
        Logger.error(s"[CohoAPIConnector] [getIncorporationStatus] - Received an error response when expecting company details - error: ${ex.getMessage}")
        CohoApiErrorResponse(ex)
    }
  }

  def getRegisteredOfficeAddress(transactionId: String)(implicit hc : HeaderCarrier): Future[CHROAddress] = {
    http.GET[CHROAddress](s"$coHoAPIUrl$coHoAPIUri/$transactionId/ro-address") recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CohoAPIConnector] [getRegisteredOfficeAddress] - Received a BadRequest status code when expecting a Registered office address")
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CohoAPIConnector] [getRegisteredOfficeAddress] - Received an error response when expecting a Registered office address - error: ${ex.getMessage}")
        throw ex
    }
  }

  def getOfficerList(transactionId: String)(implicit hc : HeaderCarrier): Future[OfficerList] = {
    http.GET[OfficerList](s"$coHoAPIUrl$coHoAPIUri/$transactionId/officer-list") recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CohoAPIConnector] [getOfficerList] - Received a BadRequest status code when expecting an Officer list")
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CohoAPIConnector] [getOfficerList] - Received an error response when expecting an Officer list - error: ${ex.getMessage}")
        throw ex
    }
  }
}
