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
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CompanyRegistrationConnector @Inject()(injMetrics: MetricsService) extends CompanyRegistrationConnect with ServicesConfig {
  lazy val companyRegistrationUrl: String = baseUrl("company-registration")
  lazy val companyRegistrationUri: String = getConfString("company-registration.uri","")
  val http = WSHttp
  val metricsService = injMetrics
}

trait CompanyRegistrationConnect {

  val companyRegistrationUrl : String
  val companyRegistrationUri : String
  val http : WSHttp
  val metricsService: MetricsSrv

  def getTransactionId(regId: String)(implicit hc : HeaderCarrier) : Future[String] = {
    val compRegTimer = metricsService.companyRegistrationResponseTimer.time()
    http.GET[JsObject](s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration/$regId/confirmation-references") map {
      compRegTimer.stop()
      _.\("transaction-id").get.as[String]
    } recover {
      case badRequestErr: BadRequestException =>
        Logger.error("[CompanyRegistrationConnect] [getTransactionId] - Received a BadRequest status code when expecting a transaction Id")
        compRegTimer.stop()
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CompanyRegistrationConnect] [getTransactionId] - Received an error response when expecting a transaction Id - error: ${ex.getMessage}")
        compRegTimer.stop()
        throw ex
    }
  }
}
