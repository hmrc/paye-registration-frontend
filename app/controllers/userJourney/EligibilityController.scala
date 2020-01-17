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

import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class EligibilityControllerImpl @Inject()(val messagesApi: MessagesApi,
                                          val keystoreConnector: KeystoreConnector,
                                          val authConnector: AuthConnector,
                                          val config: Configuration,
                                          val s4LService: S4LService,
                                          val companyDetailsService: CompanyDetailsService,
                                          val incorpInfoService: IncorporationInformationService,
                                          override val runModeConfiguration: Configuration, environment: Environment,
                                          val incorporationInformationConnector: IncorporationInformationConnector,
                                          val payeRegistrationService: PAYERegistrationService) extends EligibilityController with AuthRedirectUrls with ServicesConfig {
  override protected def mode: Mode = environment.mode
}

trait EligibilityController extends PayeBaseController {

  val compRegFEURL: String
  val compRegFEURI: String

  def questionnaire: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/questionnaire"))
  }
}
