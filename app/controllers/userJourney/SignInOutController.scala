/*
 * Copyright 2018 HM Revenue & Customs
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
import javax.inject.Inject

import connectors.KeystoreConnector
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import services.{CompanyDetailsService, IncorporationInformationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class SignInOutControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val authConnector: AuthConnector,
                                        val env: Environment,
                                        val config: Configuration,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val keystoreConnector: KeystoreConnector) extends SignInOutController with AuthRedirectUrls

trait SignInOutController extends PayeBaseController {

  val compRegFEURL: String
  val compRegFEURI: String

  def postSignIn: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/post-sign-in"))
  }

  def signOut: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/questionnaire").withNewSession)
  }

  def renewSession: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok.sendFile(new File("conf/renewSession.jpg")).as("image/jpeg"))
  }

  def destroySession: Action[AnyContent] = Action {
    Redirect(routes.SignInOutController.timeoutShow()).withNewSession
  }

  def timeoutShow = Action.async { implicit request =>
    Future.successful(Ok(views.html.timeout()))
  }
}
