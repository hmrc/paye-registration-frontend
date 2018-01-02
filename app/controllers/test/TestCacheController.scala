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

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.{BusinessRegistrationConnect, BusinessRegistrationConnector}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import services.{S4LService, S4LSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class TestCacheController @Inject()(val businessRegConnector: BusinessRegistrationConnector,
                                    val s4LService: S4LService,
                                    val messagesApi: MessagesApi) extends TestCacheCtrl {
  val authConnector = FrontendAuthConnector
}

trait TestCacheCtrl extends FrontendController with Actions with I18nSupport {

  val businessRegConnector: BusinessRegistrationConnect
  val s4LService: S4LSrv

  val tearDownS4L = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          profile <- businessRegConnector.retrieveCurrentProfile
          res     <- doTearDownS4L(profile.registrationID)
        } yield Ok(res)
  }

  protected[controllers] def doTearDownS4L(regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    s4LService.clear(regId: String) map (_ => "Save4Later cleared")
  }
}
