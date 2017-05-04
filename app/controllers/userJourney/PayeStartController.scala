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
import enums.{AccountTypes, DownstreamOutcome}
import models.external.CurrentProfile
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

@Singleton
class PayeStartController @Inject()(injCurrentProfileService: CurrentProfileService,
                                    injCoHoAPIService: IncorporationInformationService,
                                    injPayeRegistrationService: PAYERegistrationService,
                                    injMessagesApi: MessagesApi) extends PayeStartCtrl with ServicesConfig {
  val authConnector = FrontendAuthConnector
  val messagesApi = injMessagesApi
  val currentProfileService = injCurrentProfileService
  val coHoAPIService = injCoHoAPIService
  val payeRegistrationService = injPayeRegistrationService
  lazy val compRegFEURL = getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = getConfString("company-registration-frontend.www.uri", "")
}

trait PayeStartCtrl extends FrontendController with Actions with I18nSupport {

  val currentProfileService: CurrentProfileSrv
  val coHoAPIService: IncorporationInformationSrv
  val payeRegistrationService: PAYERegistrationSrv
  val compRegFEURL: String
  val compRegFEURI: String


  val startPaye = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        hasOrgAffinity {
          checkAndStoreCurrentProfile {
            profile =>
              checkAndStoreCompanyDetails(profile.registrationID) {
                assertPAYERegistrationFootprint(profile.registrationID, profile.companyTaxRegistration.transactionId){
                  Redirect(controllers.userJourney.routes.WelcomeController.show())
                }
              }
          }
        }
  }

  private def checkAndStoreCurrentProfile(f: => CurrentProfile => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    currentProfileService.fetchAndStoreCurrentProfile flatMap {
      case CurrentProfile(_, _, profile, _) if profile.status equals "draft" => Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/start"))
      case currentProfile => f(currentProfile)
    } recover {
      case ex: NotFoundException => Redirect(s"$compRegFEURL$compRegFEURI/start")
      case _ => InternalServerError(views.html.pages.error.restart())
    }
  }

  private def checkAndStoreCompanyDetails(regId: String)(f: => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    coHoAPIService.fetchAndStoreCoHoCompanyDetails(regId) flatMap {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => Future.successful(InternalServerError(views.html.pages.error.restart()))
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