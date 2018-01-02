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

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors._
import enums.{AccountTypes, CacheKeys, DownstreamOutcome, RegistrationDeletion}
import models.external.CurrentProfile
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class PayeStartController @Inject()(val currentProfileService: CurrentProfileService,
                                    val payeRegistrationService: PAYERegistrationService,
                                    val keystoreConnector: KeystoreConnector,
                                    val businessRegistrationConnector: BusinessRegistrationConnector,
                                    val companyRegistrationConnector: CompanyRegistrationConnector,
                                    val messagesApi: MessagesApi) extends PayeStartCtrl with ServicesConfig {
  val authConnector = FrontendAuthConnector
  lazy val compRegFEURL = getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = getConfString("company-registration-frontend.www.uri", "")
}

trait PayeStartCtrl extends FrontendController with Actions with I18nSupport {

  val currentProfileService: CurrentProfileSrv
  val payeRegistrationService: PAYERegistrationSrv
  val keystoreConnector: KeystoreConnect
  val businessRegistrationConnector: BusinessRegistrationConnect
  val companyRegistrationConnector: CompanyRegistrationConnect
  val compRegFEURL: String
  val compRegFEURI: String


  val startPaye = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        hasOrgAffinity {
          checkAndStoreCurrentProfile { profile =>
            assertPAYERegistrationFootprint(profile.registrationID, profile.companyTaxRegistration.transactionId){
              Redirect(controllers.userJourney.routes.WelcomeController.show())
            }
          }
        }
  }

  def restartPaye: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          (regId, txId)   <- getRegIdAndTxId
          deleted         <- payeRegistrationService.deletePayeRegistrationDocument(regId, txId)
        } yield {
          deleted match {
            case RegistrationDeletion.success => Redirect(routes.PayeStartController.startPaye())
            case RegistrationDeletion.invalidStatus => Redirect(controllers.userJourney.routes.DashboardController.dashboard())
          }
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

  private def hasOrgAffinity(f: => Future[Result])(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Result] = {
    payeRegistrationService.getAccountAffinityGroup flatMap {
      case AccountTypes.Organisation => Logger.info("[PayeStartController] - [hasOrgAffinity] - Authenticated user has ORGANISATION account, proceeding")
        f
      case AccountTypes.InvalidAccountType => Logger.info("[PayeStartController] - [hasOrgAffinity] - AUTHENTICATED USER NOT AN ORGANISATION ACCOUNT; redirecting to create new account")
        Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/post-sign-in"))
    }
  }
}
