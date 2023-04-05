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

package controllers.userJourney

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.mvc.MessagesControllerComponents
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DashboardController @Inject()(val keystoreConnector: KeystoreConnector,
                                        val authConnector: AuthConnector,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val payeRegistrationService: PAYERegistrationService,
                                        mcc: MessagesControllerComponents
                                       )(val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

   lazy val companyRegUrl: String = appConfig.servicesConfig.getConfString("company-registration-frontend.www.url", "Could not find Company Registration Frontend URL")
   lazy val companyRegUri: String = appConfig.servicesConfig.getConfString("company-registration-frontend.www.uri", "Could not find Company Registration Frontend URI")

  def dashboard = isAuthorised { implicit request =>
    Future.successful(Redirect(s"$companyRegUrl$companyRegUri/company-registration-overview"))
  }
}
