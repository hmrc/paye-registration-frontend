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
import connectors._
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.PAYEStatus
import models.external.CurrentProfile
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.error.submissionTimeout
import views.html.pages.{summary => SummaryPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryController @Inject()(val summaryService: SummaryService,
                                  val submissionService: SubmissionService,
                                  val keystoreConnector: KeystoreConnector,
                                  val authConnector: AuthConnector,
                                  val s4LService: S4LService,
                                  val companyDetailsService: CompanyDetailsService,
                                  val incorpInfoService: IncorporationInformationService,
                                  val payeRegistrationConnector: PAYERegistrationConnector,
                                  val emailService: EmailService,
                                  val incorporationInformationConnector: IncorporationInformationConnector,
                                  val payeRegistrationService: PAYERegistrationService,
                                  mcc: MessagesControllerComponents,
                                  SummaryPage: SummaryPage,
                                  submissionTimeout: submissionTimeout
                                 )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  def summary: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      invalidSubmissionGuard(profile) {
        (for {
          _ <- emailService.primeEmailData(profile.registrationID)
          employment <- summaryService.getEmploymentSectionSummary(profile.registrationID, profile.companyTaxRegistration.transactionId)
          completion <- summaryService.getCompletionCapacitySummary(profile.registrationID)
          companyDetails <- summaryService.getCompanyDetailsSummary(profile.registrationID)
          businessContact <- summaryService.getBusinessContactSummary(profile.registrationID)
          directors <- summaryService.getDirectorsSummary(profile.registrationID)
          contactDetails <- summaryService.getContactDetailsSummary(profile.registrationID)
        } yield {
          Ok(SummaryPage(employment, completion, companyDetails, businessContact, directors, contactDetails))
        })
      }
  }

  def submitRegistration: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      invalidSubmissionGuard(profile) {
        submissionService.submitRegistration(profile) map {
          case Success => Redirect(controllers.userJourney.routes.ConfirmationController.showConfirmation)
          case Cancelled => Redirect(controllers.userJourney.routes.DashboardController.dashboard)
          case Failed => Redirect(controllers.errors.routes.ErrorController.failedSubmission)
          case TimedOut => InternalServerError(submissionTimeout())
        }
      }
  }

  private[controllers] def invalidSubmissionGuard(profile: CurrentProfile)(f: => Future[Result])
                                                 (implicit hc: HeaderCarrier, request: Request[_]) = {
    payeRegistrationConnector.getRegistration(profile.registrationID) flatMap { regDoc =>
      regDoc.status match {
        case PAYEStatus.draft => f
        case PAYEStatus.held | PAYEStatus.submitted => Future.successful(Redirect(routes.ConfirmationController.showConfirmation))
        case PAYEStatus.invalid => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible))
        case PAYEStatus.rejected => Future.successful(Redirect(controllers.errors.routes.ErrorController.ineligible))
      }
    }
  }
}
