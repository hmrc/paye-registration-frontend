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
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{SummaryService, SummarySrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.{summary => SummaryPage}

@Singleton
class SummaryController @Inject()(injSummaryService: SummaryService,
                                  injKeystoreConnector: KeystoreConnector,
                                  injMessagesApi: MessagesApi)
  extends SummaryCtrl {
  val authConnector = FrontendAuthConnector
  val summaryService = injSummaryService
  val keystoreConnector = injKeystoreConnector
  val messagesApi = injMessagesApi
}

trait SummaryCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val summaryService: SummarySrv
  val keystoreConnector: KeystoreConnect

  val summary = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      summaryService.getRegistrationSummary(profile.registrationID) map {
        summaryModel => Ok(SummaryPage(summaryModel))
      } recover {
        case _ => InternalServerError(views.html.pages.error.restart())
      }
    }
  }

  val submitRegistration = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      summaryService.submitRegistration(profile.registrationID) map {
        _ => Redirect(controllers.userJourney.routes.ConfirmationController.showConfirmation)
      }
    }
  }
}
