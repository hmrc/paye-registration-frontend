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

package controllers.test

import javax.inject.Inject
import connectors.{BusinessRegistrationConnector, IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class TestCacheControllerImpl @Inject()(val businessRegConnector: BusinessRegistrationConnector,
                                        val s4LService: S4LService,
                                        val authConnector: AuthConnector,
                                        val config: Configuration,
                                        val keystoreConnector: KeystoreConnector,
                                        val companyDetailsService: CompanyDetailsService,
                                        val incorpInfoService: IncorporationInformationService,
                                        val messagesApi: MessagesApi,
                                        val incorporationInformationConnector: IncorporationInformationConnector,
                                        val payeRegistrationService: PAYERegistrationService) extends TestCacheController with AuthRedirectUrls

trait TestCacheController extends PayeBaseController {
  val businessRegConnector: BusinessRegistrationConnector
  val s4LService: S4LService

  def tearDownS4L: Action[AnyContent] = isAuthorised { implicit request =>
    for {
      profile <- businessRegConnector.retrieveCurrentProfile
      res     <- doTearDownS4L(profile.registrationID)
    } yield Ok(res)
  }

  protected[controllers] def doTearDownS4L(regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    s4LService.clear(regId: String) map (_ => "Save4Later cleared")
  }
}
