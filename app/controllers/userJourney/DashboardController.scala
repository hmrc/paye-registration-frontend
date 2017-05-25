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

package controllers.userJourney

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnector}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class DashboardController @Inject()(injMessagesApi: MessagesApi,
                                    injKeystoreConnector: KeystoreConnector,
                                    injPayeRegistrationConnector: PAYERegistrationConnector) extends DashboardCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val messagesApi = injMessagesApi
  val payeRegistrationConnector = injPayeRegistrationConnector
  override lazy val companyRegUrl = getConfString("company-registration-frontend.www.url", "Could not find Company Registration Frontend URL")
  override lazy val companyRegUri = getConfString("company-registration-frontend.www.uri", "Could not find Company Registration Frontend URI")
}

trait DashboardCtrl extends FrontendController with Actions with I18nSupport with SessionProfile with ServicesConfig {
  val keystoreConnector: KeystoreConnect
  val companyRegUrl : String
  val companyRegUri : String

  val dashboard = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user =>
    implicit request =>
      Future.successful(Redirect(s"$companyRegUrl$companyRegUri/dashboard"))
  }
}
