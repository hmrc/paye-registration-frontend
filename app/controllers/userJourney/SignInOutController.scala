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

import auth.PAYERegime
import javax.inject.{Inject, Singleton}

import config.FrontendAuthConnector
import enums.{AccountTypes, DownstreamOutcome}
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import play.api.i18n.{I18nSupport, MessagesApi}
import services._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SignInOutController @Inject()(
                                     injCurrentProfileService: CurrentProfileService,
                                     injCoHoAPIService: CoHoAPIService,
                                     injPayeRegistrationService: PAYERegistrationService,
                                     injMessagesApi: MessagesApi)
  extends SignInOutCtrl with ServicesConfig {
  val authConnector = FrontendAuthConnector
  val currentProfileService = injCurrentProfileService
  val coHoAPIService = injCoHoAPIService
  val payeRegistrationService = injPayeRegistrationService
  val messagesApi = injMessagesApi
  val compRegBaseURL = baseUrl("company-registration-frontend")
}

trait SignInOutCtrl extends FrontendController with Actions with I18nSupport {

  val currentProfileService: CurrentProfileSrv
  val coHoAPIService: CoHoAPISrv
  val payeRegistrationService: PAYERegistrationSrv
  val compRegBaseURL: String

  def postSignIn = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        hasOrgAffinity {
          checkAndStoreCurrentProfile {
            checkAndStoreCompanyDetails {
              assertPAYERegistrationFootprint {
                Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
              }
            }
          }
        }
  }

  private def checkAndStoreCurrentProfile(f: => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    currentProfileService.fetchAndStoreCurrentProfile flatMap {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => Future.successful(InternalServerError(views.html.pages.error.restart()))
    }
  }

  private def checkAndStoreCompanyDetails(f: => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    coHoAPIService.fetchAndStoreCoHoCompanyDetails flatMap {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => Future.successful(InternalServerError(views.html.pages.error.restart()))
    }
  }

  private def assertPAYERegistrationFootprint(f: => Result)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    payeRegistrationService.assertRegistrationFootprint map {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
    }
  }

  private def hasOrgAffinity(f: => Future[Result])(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Result] = {
    payeRegistrationService.getAccountAffinityGroup flatMap {
      case AccountTypes.Organisation => Logger.info("[SignInOutController] - [hasOrgAffinity] - Authenticated user has ORGANISATION account, proceeding")
        f
      case AccountTypes.InvalidAccountType => Logger.info("[SignInOutController] - [hasOrgAffinity] - AUTHENTICATED USER NOT AN ORGANISATION ACCOUNT; redirecting to create new account")
        Future.successful(Redirect(s"$compRegBaseURL/register-your-company/post-sign-in"))
    }
  }
}
