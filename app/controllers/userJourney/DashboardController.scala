/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.userJourney

import javax.inject.Inject
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.{Configuration, Environment}
import play.api.i18n.MessagesApi
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class DashboardControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val keystoreConnector: KeystoreConnector,
                                        val authConnector: AuthConnector,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val payeRegistrationService: PAYERegistrationService,
                                        override val runModeConfiguration: Configuration, environment: Environment) extends DashboardController with AuthRedirectUrls with ServicesConfig {

  override lazy val companyRegUrl = getConfString("company-registration-frontend.www.url", "Could not find Company Registration Frontend URL")
  override lazy val companyRegUri = getConfString("company-registration-frontend.www.uri", "Could not find Company Registration Frontend URI")
  override val config: Configuration = runModeConfiguration
  override protected def mode = environment.mode
}

trait DashboardController extends PayeBaseController {
  val companyRegUrl: String
  val companyRegUri: String

  def dashboard = isAuthorised { implicit request =>
    Future.successful(Redirect(s"$companyRegUrl$companyRegUri/company-registration-overview"))
  }
}
