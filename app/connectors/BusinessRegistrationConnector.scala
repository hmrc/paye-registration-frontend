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
import models.external.CurrentProfile
import play.api.Logger
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessRegistrationConnector @Inject()(injMetrics: MetricsService) extends BusinessRegistrationConnect with ServicesConfig {
  val businessRegUrl = baseUrl("business-registration")
  val http : WSHttp = WSHttp
  val metricsService = injMetrics
}

sealed trait BusinessRegistrationResponse
case class BusinessRegistrationSuccessResponse(response: CurrentProfile) extends BusinessRegistrationResponse
case object BusinessRegistrationNotFoundResponse extends BusinessRegistrationResponse
case object BusinessRegistrationForbiddenResponse extends BusinessRegistrationResponse
case class BusinessRegistrationErrorResponse(err: Exception) extends BusinessRegistrationResponse

trait BusinessRegistrationConnect {

  val businessRegUrl: String
  val http: WSHttp
  val metricsService: MetricsSrv

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[CurrentProfile]): Future[BusinessRegistrationResponse] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[CurrentProfile](s"$businessRegUrl/business-registration/business-tax-registration") map {
      currentProfile =>
        businessRegistrationTimer.stop()
        BusinessRegistrationSuccessResponse(currentProfile)
    } recover {
      case e: NotFoundException =>
        Logger.error(s"[BusinessRegistrationConnector] [retrieveCurrentProfile] - Received a NotFound status code when expecting current profile from Business-Registration")
        businessRegistrationTimer.stop()
        BusinessRegistrationNotFoundResponse
      case e: ForbiddenException =>
        Logger.error(s"[BusinessRegistrationConnector] [retrieveCurrentProfile] - Received a Forbidden status code when expecting current profile from Business-Registration")
        businessRegistrationTimer.stop()
        BusinessRegistrationForbiddenResponse
      case e: Exception =>
        Logger.error(s"[BusinessRegistrationConnector] [retrieveCurrentProfile] - Received error when expecting current profile from Business-Registration - Error ${e.getMessage}")
        businessRegistrationTimer.stop()
        BusinessRegistrationErrorResponse(e)
    }
  }
}
