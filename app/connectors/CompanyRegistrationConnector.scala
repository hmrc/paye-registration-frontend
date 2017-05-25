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
import models.external.CompanyRegistrationProfile
import play.api.Logger
import play.api.libs.json._
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CompanyRegistrationConnector @Inject()(injFeatureSwitch: PAYEFeatureSwitch,
                                             injMetrics: MetricsService) extends CompanyRegistrationConnect with ServicesConfig {
  lazy val companyRegistrationUrl: String = baseUrl("company-registration")
  lazy val companyRegistrationUri: String = getConfString("company-registration.uri","")
  lazy val stubUrl: String = baseUrl("incorporation-frontend-stubs")
  lazy val stubUri: String = getConfString("incorporation-frontend-stubs.uri","")
  val http = WSHttp
  val metricsService = injMetrics
  val featureSwitch = injFeatureSwitch
}

trait CompanyRegistrationConnect {

  val companyRegistrationUrl : String
  val companyRegistrationUri : String
  val stubUrl : String
  val stubUri : String
  val http : WSHttp
  val metricsService: MetricsSrv
  val featureSwitch: PAYEFeatureSwitches

  def getCompanyRegistrationDetails(regId: String)(implicit hc : HeaderCarrier) : Future[CompanyRegistrationProfile] = {
    val companyRegTimer = metricsService.companyRegistrationResponseTimer.time()

    val url = if (useCompanyRegistration) s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration" else s"$stubUrl$stubUri"

    http.GET[JsObject](s"$url/$regId/corporation-tax-registration") map {
      response =>
        companyRegTimer.stop()
        val status = (response \ "status").as[String]
        val txId = (response \ "confirmationReferences" \ "transaction-id").as[String]
        CompanyRegistrationProfile(status, txId)
    } recover {
      case badRequestErr: BadRequestException =>
        companyRegTimer.stop()
        Logger.error("[CompanyRegistrationConnect] [getCompanyRegistrationDetails] - Received a BadRequest status code when expecting a Company Registration document")
        throw badRequestErr
      case ex: Exception =>
        companyRegTimer.stop()
        Logger.error(s"[CompanyRegistrationConnect] [getCompanyRegistrationDetails] - Received an error response when expecting a Company Registration document - error: ${ex.getMessage}")
        throw ex
    }
  }

  private[connectors] def useCompanyRegistration: Boolean = {
    featureSwitch.companyReg.enabled
  }

}
