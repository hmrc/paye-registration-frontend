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

import common.exceptions.DownstreamExceptions.ConfirmationRefsNotFoundException
import connectors._
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.{CacheKeys, DownstreamOutcome, RegistrationDeletion}
import javax.inject.Inject
import models.external.CurrentProfile
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.PAYEFeatureSwitches

import scala.concurrent.Future

class PayeStartControllerImpl @Inject()(val currentProfileService: CurrentProfileService,
                                        val payeRegistrationService: PAYERegistrationService,
                                        val keystoreConnector: KeystoreConnector,
                                        val authConnector: AuthConnector,
                                        val config: Configuration,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val businessRegistrationConnector: BusinessRegistrationConnector,
                                        val companyRegistrationConnector: CompanyRegistrationConnector,
                                        val featureSwitches: PAYEFeatureSwitches,
                                        val messagesApi: MessagesApi,
                                        val incorporationInformationConnector: IncorporationInformationConnector) extends PayeStartController with AuthRedirectUrls

trait PayeStartController extends PayeBaseController {
  val currentProfileService: CurrentProfileService
  val payeRegistrationService: PAYERegistrationService
  val businessRegistrationConnector: BusinessRegistrationConnector
  val companyRegistrationConnector: CompanyRegistrationConnector

  val payeRegElFEURL: String
  val payeRegElFEURI: String

  def steppingStone(): Action[AnyContent] = Action { implicit request =>
      Redirect(s"$payeRegElFEURL$payeRegElFEURI")
  }

  val startPaye = isAuthorisedAndIsOrg { implicit request =>
    checkAndStoreCurrentProfile { profile =>
      assertPAYERegistrationFootprint(profile.registrationID, profile.companyTaxRegistration.transactionId) {
            Redirect(routes.EmploymentController.paidEmployees())
      }
    }
  }

  def restartPaye: Action[AnyContent] = isAuthorised { implicit request =>
    for {
      (regId, txId) <- getRegIdAndTxId
      deleted       <- payeRegistrationService.deleteRejectedRegistration(regId, txId)
    } yield deleted match {
      case RegistrationDeletion.success       => Redirect(routes.PayeStartController.startPaye())
      case RegistrationDeletion.invalidStatus => Redirect(controllers.userJourney.routes.DashboardController.dashboard())
    }
  }

  private def getRegIdAndTxId(implicit hc: HeaderCarrier): Future[(String, String)] = {
    keystoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString) flatMap {
      case Some(profile) => Future.successful((profile.registrationID, profile.companyTaxRegistration.transactionId))
      case None => for {
        businessProfile <- businessRegistrationConnector.retrieveCurrentProfile
        companyProfile  <- companyRegistrationConnector.getCompanyRegistrationDetails(businessProfile.registrationID)
      } yield {
        (businessProfile.registrationID, companyProfile.transactionId)
      }
    }
  }

  private def checkAndStoreCurrentProfile(f: => CurrentProfile => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    currentProfileService.fetchAndStoreCurrentProfile flatMap { currentProfile: CurrentProfile =>
      currentProfileChecks(currentProfile)(f)
    } recover {
      case _: NotFoundException                 => Redirect("https://www.tax.service.gov.uk/business-registration/select-taxes")
      case _: ConfirmationRefsNotFoundException => Redirect("https://www.tax.service.gov.uk/business-registration/select-taxes")
      case _                                    => InternalServerError(views.html.pages.error.restart())
    }
  }

  private def assertPAYERegistrationFootprint(regId: String, txId: String)(f: => Result)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    payeRegistrationService.assertRegistrationFootprint(regId, txId) map {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
    }
  }
}
