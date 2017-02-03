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
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector}
import enums.DownstreamOutcome
import forms.companyDetails.TradingNameForm
import models.view.TradingName
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import services.{CoHoAPIService, CoHoAPISrv, CompanyDetailsService, CompanyDetailsSrv, S4LService, S4LSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.companyDetails.{tradingName => TradingNamePage}

import scala.concurrent.Future

@Singleton
class CompanyDetailsController @Inject()(
                                          injS4LService: S4LService,
                                          injKeystoreConnector: KeystoreConnector,
                                          injCompanyDetailsService: CompanyDetailsService,
                                          injCohoService: CoHoAPIService,
                                          injMessagesApi: MessagesApi)
  extends CompanyDetailsCtrl {
  val authConnector = FrontendAuthConnector
  val s4LService = injS4LService
  val keystoreConnector = injKeystoreConnector
  val companyDetailsService = injCompanyDetailsService
  val cohoService = injCohoService
  val messagesApi = injMessagesApi
}

trait CompanyDetailsCtrl extends FrontendController with Actions with I18nSupport {
  val s4LService: S4LSrv
  val keystoreConnector: KeystoreConnect
  val companyDetailsService: CompanyDetailsSrv
  val cohoService: CoHoAPISrv

  val tradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>

    for {
      oCompanyDetails   <- companyDetailsService.getCompanyDetails()
      companyName       <- companyDetailsService.getCompanyName(oCompanyDetails)
    } yield oCompanyDetails flatMap (_.tradingName) match {
      case Some(model) => Ok(TradingNamePage(TradingNameForm.form.fill(model), companyName))
      case _ => Ok(TradingNamePage(TradingNameForm.form, companyName))
    }
  }

  val submitTradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>

    TradingNameForm.form.bindFromRequest.fold(
      errors => badRequestResponse(errors),
      success => {
        val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
        if (validatedForm.hasErrors) {
          badRequestResponse(validatedForm)
        } else {
          companyDetailsService.submitTradingName(success) map {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.EmploymentController.employingStaff())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        }
      }
    )
  }

  private def badRequestResponse(form: Form[TradingName])(implicit request: Request[AnyContent]): Future[Result] = {
    cohoService.getStoredCompanyName map {
      cName => BadRequest(TradingNamePage(form, cName))
    }
  }

}
