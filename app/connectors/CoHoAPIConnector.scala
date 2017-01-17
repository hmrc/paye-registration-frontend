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

import config.WSHttp
import common.exceptions.InternalExceptions._
import models.external.CoHoCompanyDetailsModel
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object CoHoAPIConnector extends CoHoAPIConnector with ServicesConfig {
  //$COVERAGE-OFF$
  val coHoAPIUrl = baseUrl("coho-api")
  val http = WSHttp
  //$COVERAGE-ON$
}

sealed trait CohoApiResponse
case class CohoApiSuccessResponse(response: CoHoCompanyDetailsModel) extends CohoApiResponse
case object CohoApiBadRequestResponse extends CohoApiResponse
case class CohoApiErrorResponse(ex: Exception) extends CohoApiResponse

trait CoHoAPIConnector {

  val coHoAPIUrl: String
  val http: HttpGet with HttpPost

  def getCoHoCompanyDetails(registrationID: String)(implicit hc: HeaderCarrier): Future[CohoApiResponse] = {
    http.GET[CoHoCompanyDetailsModel](s"$coHoAPIUrl/incorporation-frontend-stubs/company/$registrationID") map {
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
}
