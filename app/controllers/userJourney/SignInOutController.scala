/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

class SignInOutControllerImpl @Inject()(val authConnector: AuthConnector,
                                        val s4LService: S4LService,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val keystoreConnector: KeystoreConnector,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val payeRegistrationService: PAYERegistrationService,
                                        mcc: MessagesControllerComponents
                                       )(implicit val appConfig: AppConfig, val ec: ExecutionContext) extends SignInOutController(mcc) with AuthRedirectUrls

abstract class SignInOutController(mcc: MessagesControllerComponents) extends PayeBaseController(mcc) {
  implicit val ec: ExecutionContext
  implicit val appConfig: AppConfig
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

  def incorporationRejected: Action[AnyContent] = isAuthorised {
    implicit request =>
      Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/cant-continue"))
  }
}
