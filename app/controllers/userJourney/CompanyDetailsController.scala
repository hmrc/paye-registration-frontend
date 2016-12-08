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
import connectors.{KeystoreConnector, S4LConnector}
import forms.companyDetails.TradingNameForm
import models.companyDetails.TradingNameModel
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object CompanyDetailsController extends CompanyDetailsController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val s4LConnector = S4LConnector
  override val keystoreConnector = KeystoreConnector
  //$COVERAGE-ON

}

trait CompanyDetailsController extends FrontendController with Actions {

  val s4LConnector: S4LConnector
  val keystoreConnector: KeystoreConnector

  val tradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    keystoreConnector.fetchAndGet[TradingNameModel]("tName"). map {
      case Some(model) => Ok(views.html.pages.companyDetails.tradingName(TradingNameForm.form.fill(model)))
      case _ => Ok(views.html.pages.companyDetails.tradingName(TradingNameForm.form))
    }
  }

  val submitTradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    TradingNameForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(views.html.pages.companyDetails.tradingName(errors))),
      success => {
        val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
        if(validatedForm.hasErrors) {
          Future.successful(BadRequest(views.html.pages.companyDetails.tradingName(validatedForm)))
        } else {
          Future.successful(Redirect(controllers.userJourney.routes.WelcomeController.show()))
        }
      }
    )
  }

}
