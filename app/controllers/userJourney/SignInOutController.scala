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
import com.google.inject.{Inject, Singleton}
import config.{PAYEShortLivedCache, PAYESessionCache, FrontendAuthConnector}
import connectors._
import enums.DownstreamOutcome
import play.api.mvc.{AnyContent, Request, Result}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

//object SignInOutController extends SignInOutController {
//  //$COVERAGE-OFF$
//  override val authConnector = FrontendAuthConnector
//  override val currentProfileService = new CurrentProfileService(new KeystoreConnector(new PAYESessionCache()), new BusinessRegistrationConnector())
//  override val coHoAPIService = new CoHoAPIService(new KeystoreConnector(new PAYESessionCache()), new CoHoAPIConnector())
//  override val payeRegistrationService = new PAYERegistrationService(new KeystoreConnector(new PAYESessionCache), new PAYERegistrationConnector(), new S4LService(new S4LConnector(new PAYEShortLivedCache()), new KeystoreConnector(new PAYESessionCache())))
//  //$COVERAGE-ON$
//}
@Singleton
class SignInOutController @Inject()(
                                     injCurrentProfileService: CurrentProfileService,
                                     injCoHoAPIService: CoHoAPIService,
                                     injPayeRegistrationService: PAYERegistrationService) extends SignInOutCtrl {
  val authConnector = FrontendAuthConnector
  val currentProfileService = injCurrentProfileService
  val coHoAPIService = injCoHoAPIService
  val payeRegistrationService = injPayeRegistrationService
}

trait SignInOutCtrl extends FrontendController with Actions {

  val currentProfileService: CurrentProfileSrv
  val coHoAPIService: CoHoAPISrv
  val payeRegistrationService: PAYERegistrationService

  def postSignIn = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        checkAndStoreCurrentProfile {
          checkAndStoreCompanyDetails {
            assertPAYERegistrationFootprint {
              Redirect(controllers.userJourney.routes.CompanyDetailsController.tradingName())
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
}
