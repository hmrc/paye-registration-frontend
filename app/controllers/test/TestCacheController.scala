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

package controllers.test

import auth.test.TestPAYERegime
import javax.inject.{Inject, Singleton}
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector}
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{S4LService, S4LSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class TestCacheController @Inject()(injKeystoreConnector: KeystoreConnector,
                                    injS4LService: S4LService,
                                    injMessagesApi: MessagesApi)
  extends TestCacheCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val s4LService = injS4LService
  val messagesApi = injMessagesApi
}

trait TestCacheCtrl extends FrontendController with Actions with I18nSupport {

  val keystoreConnector: KeystoreConnect
  val s4LService: S4LSrv

  val tearDownS4L = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        s4LService.clear() map {
          response => Ok("Save4Later cleared")
        }
  }
}
