/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.templates.timeout

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignInOutController @Inject()(val authConnector: AuthConnector,
                                    val s4LService: S4LService,
                                    val companyDetailsService: CompanyDetailsService,
                                    val incorpInfoService: IncorporationInformationService,
                                    val keystoreConnector: KeystoreConnector,
                                    val incorporationInformationConnector: IncorporationInformationConnector,
                                    val payeRegistrationService: PAYERegistrationService,
                                    mcc: MessagesControllerComponents,
                                    timeout: timeout
                                   )(implicit val appConfig: AppConfig, val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  def postSignIn: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/post-sign-in"))
  }

  def signOut: Action[AnyContent] = Action.async {
    _ => Future.successful(Redirect(appConfig.betaFeedbackUrl).withNewSession)
  }

  def renewSession: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok.sendFile(new File("conf/renewSession.jpg")).as("image/jpeg"))
  }

  def destroySession: Action[AnyContent] = Action.async {
    _ => Future.successful(Redirect(routes.SignInOutController.timeoutShow).withNewSession)
  }

  def timeoutShow: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(timeout()))
  }

  def incorporationRejected: Action[AnyContent] = isAuthorised {
    implicit request =>
      Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/cant-continue"))
  }
}
