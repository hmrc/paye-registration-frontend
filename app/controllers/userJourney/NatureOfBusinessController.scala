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
import forms.natureOfBuinessDetails.NatureOfBusinessForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.{natureOfBusiness => NatureOfBusinessPage}

import scala.concurrent.Future

@Singleton
class NatureOfBusinessController @Inject()(injMessagesApi: MessagesApi) extends NatureOfBusinessCtrl {
  val authConnector = FrontendAuthConnector
  implicit val messagesApi = injMessagesApi
}

trait NatureOfBusinessCtrl extends FrontendController with Actions with I18nSupport {

  val authConnector: AuthConnector
  implicit val messagesApi: MessagesApi

  val natureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(NatureOfBusinessPage(NatureOfBusinessForm.form, "Company limited")))
  }

  val submitNatureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        NatureOfBusinessForm.form.bindFromRequest.fold(
          errors => Future.successful(Ok(NatureOfBusinessPage(errors, "Company Limited"))),
          valid => Future.successful(Ok(NatureOfBusinessPage(NatureOfBusinessForm.form, "Company Limited")))
        )
  }
}
