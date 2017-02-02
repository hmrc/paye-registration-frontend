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
import config.{PAYEShortLivedCache, FrontendAuthConnector, PAYESessionCache}
import connectors.{S4LConnector, KeystoreConnector}
import services.S4LService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object TestCacheController extends TestCacheController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val keystoreConnector = new KeystoreConnector(new PAYESessionCache())
  override val s4LService = new S4LService(new S4LConnector(new PAYEShortLivedCache()), new KeystoreConnector(new PAYESessionCache()))
  //$COVERAGE-ON$

}

trait TestCacheController extends FrontendController with Actions {

  val keystoreConnector: KeystoreConnector
  val s4LService: S4LService

  val tearDownS4L = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    s4LService.clear() map {
      response => Ok("Save4Later cleared")
    }
  }

}
