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
import connectors._
import enums.PAYEStatus
import models.external.CurrentProfile
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Result
import services.{SubmissionService, SubmissionSrv, SummaryService, SummarySrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.SessionProfile
import views.html.pages.{summary => SummaryPage}

import scala.concurrent.Future

@Singleton
class SummaryController @Inject()(injSummaryService: SummaryService,
                                  injSubmissionService: SubmissionService,
                                  injKeystoreConnector: KeystoreConnector,
                                  injPayeRegistrationConnector: PAYERegistrationConnector,
                                  injMessagesApi: MessagesApi)
  extends SummaryCtrl {
  val authConnector = FrontendAuthConnector
  val summaryService = injSummaryService
  val submissionService = injSubmissionService
  val keystoreConnector = injKeystoreConnector
  val payeRegistrationConnector = injPayeRegistrationConnector
  val messagesApi = injMessagesApi
}

trait SummaryCtrl extends FrontendController with Actions with I18nSupport with SessionProfile with ServicesConfig {

  val summaryService: SummarySrv
  val submissionService: SubmissionSrv
  val keystoreConnector: KeystoreConnect
  val payeRegistrationConnector: PAYERegistrationConnect

  val summary = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      invalidSubmissionGuard(profile) {
        summaryService.getRegistrationSummary(profile.registrationID) map {
          summaryModel => Ok(SummaryPage(summaryModel))
        } recover {
          case _ => InternalServerError(views.html.pages.error.restart())
        }
      }
    }
  }

  val submitRegistration = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      invalidSubmissionGuard(profile) {
        submissionService.submitRegistration(profile) map {
          case Success => Redirect(controllers.userJourney.routes.ConfirmationController.showConfirmation())
          case Cancelled => Redirect(controllers.userJourney.routes.DashboardController.dashboard())
          case Failed => Redirect(controllers.errors.routes.ErrorController.failedSubmission())
          case TimedOut => InternalServerError(views.html.pages.error.submissionTimeout())
        }
      }
    }
  }

  private[controllers] def invalidSubmissionGuard(profile: CurrentProfile)(f: => Future[Result])(implicit hc: HeaderCarrier) = {
    payeRegistrationConnector.getRegistration(profile.registrationID) flatMap { regDoc =>
      regDoc.status match {
        case PAYEStatus.draft => f
        case PAYEStatus.held | PAYEStatus.submitted => Future.successful(Redirect(routes.ConfirmationController.showConfirmation()))
        case PAYEStatus.invalid => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible()))
          //TODO: Potentially need a new view to better demonstrate the problem
        case PAYEStatus.rejected => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible()))
      }
    }
  }
}
