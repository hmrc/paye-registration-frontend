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
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CompanyRegistrationConnector @Inject()() extends CompanyRegistrationConnect with ServicesConfig {
  val companyRegistrationUri: String = baseUrl("company-registration")
  val http = WSHttp
}

trait CompanyRegistrationConnect {

  val companyRegistrationUri : String
  val http : WSHttp

  def getTransactionId(regId: String)(implicit hc : HeaderCarrier) : Future[String] = {
    http.GET[JsObject](s"$companyRegistrationUri/company-registration/corporation-tax-registration/$regId/confirmation-references") map {
      _.\("transaction-id").get.as[String]
    } recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CompanyRegistrationConnect] [getTransactionId] - Received a BadRequest status code when expecting a transaction Id")
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CompanyRegistrationConnect] [getTransactionId] - Received an error response when expecting a transaction Id - error: ${ex.getMessage}")
        throw ex
    }
  }
}
