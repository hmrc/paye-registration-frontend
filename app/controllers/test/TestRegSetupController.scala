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
import config.FrontendAuthConnector
import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import services.PAYERegistrationService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

object TestRegSetupController extends TestRegSetupController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val payeRegService = PAYERegistrationService
  override val payeRegConnector = PAYERegistrationConnector
  //$COVERAGE-ON$
}

trait TestRegSetupController extends FrontendController with Actions {

  val payeRegService: PAYERegistrationService
  val payeRegConnector: PAYERegistrationConnector

  val regTeardown = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    payeRegConnector.testRegistrationTeardown() map {
      case DownstreamOutcome.Success => Ok("Registration collection successfully cleared")
      case DownstreamOutcome.Failure => InternalServerError("Error clearing registration collection")
    }
  }

}
