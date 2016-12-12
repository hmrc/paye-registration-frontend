/*
 * Copyright 2016 HM Revenue & Customs
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
import config.FrontendAuthConnector
import enums.DownstreamOutcome
import play.api.mvc.{AnyContent, Request, Result}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.CurrentProfileService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object SignInOutController extends SignInOutController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val currentProfileService = CurrentProfileService
  //$COVERAGE-ON$
}

trait SignInOutController extends FrontendController with Actions {

  val currentProfileService: CurrentProfileService

  def postSignIn = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        checkAndStoreCurrentProfile {
          Redirect(controllers.userJourney.routes.CompanyDetailsController.tradingName())
        }
  }

  private def checkAndStoreCurrentProfile(f: => Result)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    currentProfileService.fetchAndStoreCurrentProfile map {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
    }
  }
}
