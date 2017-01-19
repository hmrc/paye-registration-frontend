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
import config.FrontendAuthConnector
import services.PAYERegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.{summary => SummaryPage}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

object SummaryController extends SummaryController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val payeRegistrationService = PAYERegistrationService
  //$COVERAGE-ON$
}

trait SummaryController extends FrontendController with Actions {

  val payeRegistrationService: PAYERegistrationService

  val summary = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      oSummaryModel <- payeRegistrationService.getRegistrationSummary
    } yield oSummaryModel match {
      case Some(summaryModel) => Ok(SummaryPage(summaryModel))
      case None => InternalServerError(views.html.pages.error.restart())
    }
  }
}
