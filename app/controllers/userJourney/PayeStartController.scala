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
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.{CacheKeys, DownstreamOutcome, RegistrationDeletion}
import models.external.CurrentProfile
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class PayeStartControllerImpl @Inject()(val currentProfileService: CurrentProfileService,
                                        val payeRegistrationService: PAYERegistrationService,
                                        val keystoreConnector: KeystoreConnector,
                                        val authConnector: AuthConnector,
                                        val env: Environment,
                                        val config: Configuration,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val businessRegistrationConnector: BusinessRegistrationConnector,
                                        val companyRegistrationConnector: CompanyRegistrationConnector,
                                        val messagesApi: MessagesApi) extends PayeStartController with AuthRedirectUrls

trait PayeStartController extends PayeBaseController {
  val currentProfileService: CurrentProfileService
  val payeRegistrationService: PAYERegistrationService
  val businessRegistrationConnector: BusinessRegistrationConnector
  val companyRegistrationConnector: CompanyRegistrationConnector

  val compRegFEURL: String
  val compRegFEURI: String


  val startPaye = isAuthorisedAndIsOrg { implicit request =>
    checkAndStoreCurrentProfile { profile =>
      assertPAYERegistrationFootprint(profile.registrationID, profile.companyTaxRegistration.transactionId){
        Redirect(controllers.userJourney.routes.WelcomeController.show())
      }
    }
  }

  def restartPaye: Action[AnyContent] = isAuthorised { implicit request =>
    for {
      (regId, txId) <- getRegIdAndTxId
      deleted       <- payeRegistrationService.deletePayeRegistrationDocument(regId, txId)
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
    currentProfileService.fetchAndStoreCurrentProfile flatMap {
      case CurrentProfile(_, compRegProfile, _, _) if compRegProfile.status equals "draft" => Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/register"))
      case currentProfile => f(currentProfile)
    } recover {
      case ex: NotFoundException => Redirect(s"$compRegFEURL$compRegFEURI/register")
      case _ => InternalServerError(views.html.pages.error.restart())
    }
  }

  private def assertPAYERegistrationFootprint(regId: String, txId: String)(f: => Result)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    payeRegistrationService.assertRegistrationFootprint(regId, txId) map {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
    }
  }
}
