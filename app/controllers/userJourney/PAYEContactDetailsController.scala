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
import forms.payeContactDetails.PAYEContactDetailsForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.{payeContactDetails => PAYEContactDetailsPage}

import scala.concurrent.Future

@Singleton
class PAYEContactDetailsController @Inject()(
                                              injCompanyDetailsService: CompanyDetailsService,
                                              injMessagesApi: MessagesApi)
  extends PAYEContactDetailsCtrl {
  val authConnector = FrontendAuthConnector
  val companyDetailsService = injCompanyDetailsService
  val messagesApi = injMessagesApi
}

trait PAYEContactDetailsCtrl extends FrontendController with Actions with I18nSupport {

  val companyDetailsService: CompanyDetailsSrv

  val payeContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        companyDetailsService.getCompanyDetails map {
          details => Ok(PAYEContactDetailsPage(details.companyName, PAYEContactDetailsForm.form))
        }
  }

  val submitPAYEContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        PAYEContactDetailsForm.form.bindFromRequest.fold(
          errs => companyDetailsService.getCompanyDetails map (details => BadRequest(PAYEContactDetailsPage(details.companyName, errs))),
          success => Future.successful(Ok(Json.toJson(success)))
        )
  }
}
