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

import config.AppConfig
import connectors.httpParsers.CompanyRegistrationHttpParsers
import models.external.CompanyRegistrationProfile
import play.api.libs.json._
import play.api.mvc.Request
import services.MetricsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException}
import utils.PAYEFeatureSwitch

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyRegistrationConnector @Inject()(val featureSwitch: PAYEFeatureSwitch,
                                             val http: HttpClient,
                                             val metricsService: MetricsService,
                                             appConfig: AppConfig)(implicit val ec: ExecutionContext) extends BaseConnector with CompanyRegistrationHttpParsers {

  lazy val companyRegistrationUrl: String = appConfig.servicesConfig.baseUrl("company-registration")
  lazy val companyRegistrationUri: String = appConfig.servicesConfig.getString("microservice.services.company-registration.uri")
  lazy val stubUrl: String = appConfig.servicesConfig.baseUrl("incorporation-frontend-stubs")
  lazy val stubUri: String = appConfig.servicesConfig.getString("microservice.services.incorporation-frontend-stubs.uri")

  def getCompanyRegistrationDetails(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[CompanyRegistrationProfile] = {
    infoLog("[getCompanyRegistrationDetails] attempting to getCompanyRegistrationDetails")
    val url = if (useCompanyRegistration) s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration" else s"$stubUrl$stubUri"
    withTimer {
      withRecovery()("getCompanyRegistrationDetails", Some(regId)) {
        http.GET[CompanyRegistrationProfile](s"$url/$regId/corporation-tax-registration")(companyRegistrationDetailsHttpReads(regId), hc, ec)
      }
    }
  }

  def getVerifiedEmail(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] =
    withRecovery(Some(Option.empty[String]))("getVerifiedEmail", Some(regId)) {
      http.GET[Option[String]](s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration/$regId/retrieve-email")(verifiedEmailHttpReads(regId), hc, ec)
    }

  private[connectors] def useCompanyRegistration: Boolean = featureSwitch.companyReg.enabled

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics(metricsService.companyRegistrationResponseTimer.time())(f)
}