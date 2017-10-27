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

import java.io.File
import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SignInOutController @Inject()(val messagesApi: MessagesApi) extends SignInOutCtrl with ServicesConfig {
  val authConnector     = FrontendAuthConnector
  lazy val compRegFEURL = getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = getConfString("company-registration-frontend.www.uri", "")
}

trait SignInOutCtrl extends FrontendController with Actions with I18nSupport {

  val compRegFEURL: String
  val compRegFEURI: String

  val postSignIn = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Redirect(s"$compRegFEURL$compRegFEURI/post-sign-in")
  }

  def signOut: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Redirect(s"$compRegFEURL$compRegFEURI/questionnaire").withNewSession
  }

  def renewSession: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Ok.sendFile(new File("conf/renewSession.jpg")).as("image/jpeg")
  }

  def destroySession: Action[AnyContent] = Action {
    Redirect(routes.SignInOutController.timeoutShow()).withNewSession
  }

  def timeoutShow = Action.async {
    implicit request =>
      Future.successful(Ok(views.html.timeout()))
  }
}
