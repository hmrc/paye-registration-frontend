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
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class WelcomeController @Inject()(injMessagesApi: MessagesApi)
  extends WelcomeCtrl {
  val authConnector = FrontendAuthConnector
  val messagesApi = injMessagesApi
}

trait WelcomeCtrl extends FrontendController with I18nSupport with Actions {

  val show = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Ok(views.html.pages.welcome())
  }

  val submit = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Redirect(controllers.userJourney.routes.EligibilityController.companyEligibility())
  }

}
