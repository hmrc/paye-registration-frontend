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
import forms.companyDetails.TradingNameForm
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

object SignInOutController extends SignInOutController {

  val authConnector = FrontendAuthConnector

}

trait SignInOutController extends FrontendController with Actions {

  def postSignIn = AuthorisedFor(taxRegime = PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.pages.companyDetails.tradingName(TradingNameForm.form)))
  }

}
