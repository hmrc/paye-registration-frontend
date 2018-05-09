/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors._
import controllers.exceptions.{FrontendControllerException, GeneralException}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.PAYEStatus
import models.external.CurrentProfile
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.error.restart
import views.html.pages.{summary => SummaryPage}

import scala.concurrent.Future

class SummaryControllerImpl @Inject()(val summaryService: SummaryService,
                                      val submissionService: SubmissionService,
                                      val keystoreConnector: KeystoreConnector,
                                      val authConnector: AuthConnector,
                                      val config: Configuration,
                                      val s4LService: S4LService,
                                      val companyDetailsService: CompanyDetailsService,
                                      val incorpInfoService: IncorporationInformationService,
                                      val payeRegistrationConnector: PAYERegistrationConnector,
                                      val emailService: EmailService,
                                      val messagesApi: MessagesApi) extends SummaryController with AuthRedirectUrls

trait SummaryController extends PayeBaseController {
  val summaryService: SummaryService
  val submissionService: SubmissionService
  val payeRegistrationConnector: PAYERegistrationConnector
  val emailService: EmailService

  def summary: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    invalidSubmissionGuard(profile) {
      (for {
        _       <- emailService.primeEmailData(profile.registrationID)
        summary <- summaryService.getRegistrationSummary(profile.registrationID, profile.companyTaxRegistration.transactionId)
      } yield {
        Ok(SummaryPage(summary))
      }).recover{
        case e: FrontendControllerException => e.recover
        case e: Exception => GeneralException(s"an error Occured with message ${e.getMessage}").recover
      }
    }
  }

  def submitRegistration: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    invalidSubmissionGuard(profile) {
      submissionService.submitRegistration(profile) map {
        case Success    => Redirect(controllers.userJourney.routes.ConfirmationController.showConfirmation())
        case Cancelled  => Redirect(controllers.userJourney.routes.DashboardController.dashboard())
        case Failed     => Redirect(controllers.errors.routes.ErrorController.failedSubmission())
        case TimedOut   => InternalServerError(views.html.pages.error.submissionTimeout())
      }
    }
  }

  private[controllers] def invalidSubmissionGuard(profile: CurrentProfile)(f: => Future[Result])(implicit hc: HeaderCarrier) = {
    payeRegistrationConnector.getRegistration(profile.registrationID) flatMap { regDoc =>
      regDoc.status match {
        case PAYEStatus.draft                       => f
        case PAYEStatus.held | PAYEStatus.submitted => Future.successful(Redirect(routes.ConfirmationController.showConfirmation()))
        case PAYEStatus.invalid                     => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible()))
          //TODO: Potentially need a new view to better demonstrate the problem
        case PAYEStatus.rejected                    => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible()))
      }
    }
  }
}
