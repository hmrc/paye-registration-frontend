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
import connectors.KeystoreConnector
import forms.companyDetails.TradingNameForm
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{CompanyDetailsService, S4LService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.companyDetails.{tradingName => TradingNamePage}

import scala.concurrent.Future

object CompanyDetailsController extends CompanyDetailsController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val s4LService = S4LService
  override val keystoreConnector = KeystoreConnector
  override val companyDetailsService = CompanyDetailsService
  //$COVERAGE-ON$

}

trait CompanyDetailsController extends FrontendController with Actions {

  val s4LService: S4LService
  val keystoreConnector: KeystoreConnector
  val companyDetailsService: CompanyDetailsService

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

    Future.successful(Ok)
//    TradingNameForm.form.bindFromRequest.fold(
//      errors => keystoreConnector.fetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString) map {
//          companyDetails => BadRequest(views.html.pages.companyDetails.tradingName(errors, getCompanyNameFromDetails(companyDetails)))
//        },
//      (success: TradingNameFormModel) => {
//        val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
//        if(validatedForm.hasErrors) {
//          keystoreConnector.fetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString) map {
//            companyDetails => BadRequest(views.html.pages.companyDetails.tradingName(validatedForm, getCompanyNameFromDetails(companyDetails)))
//          }
//        } else {
//          s4LService.saveForm[TradingName](CacheKeys.TradingName.toString, success.toData) map {
//            cacheMap => Redirect(controllers.userJourney.routes.WelcomeController.show())
//          }
//        }
//      }
//    )
  }

//  private def getCompanyNameFromDetails(details: Option[CoHoCompanyDetailsModel]): String = {
//    details map {
//      details => details.companyName
//    } getOrElse {
//      throw new CompanyDetailsNotFoundException
//    }
//  }

}
