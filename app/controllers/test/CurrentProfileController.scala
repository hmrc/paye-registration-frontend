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
import com.google.inject.{Inject, Singleton}
import config.{FrontendAuthConnector, PAYESessionCache}
import connectors.test.TestBusinessRegConnector
import connectors.{BusinessRegistrationConnector, BusinessRegistrationSuccessResponse, KeystoreConnector}
import enums.CacheKeys
import models.external.CurrentProfile
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

//object CurrentProfileController extends CurrentProfileController {
//  //$COVERAGE-OFF$
//  override val authConnector = FrontendAuthConnector
//  override val keystoreConnector = new KeystoreConnector(new PAYESessionCache())
//  override val businessRegConnector = new BusinessRegistrationConnector()
//  override val testBusinessRegConnector = new TestBusinessRegConnector()
//  //$COVERAGE-ON$
//}
@Singleton
class CurrentProfileController @Inject()(
                                          injKeystoreConnector: KeystoreConnector,
                                          injBusinessRegConnector: BusinessRegistrationConnector,
                                          injTestBusinessRegConnector: TestBusinessRegConnector)
  extends CurrentProfileCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val businessRegConnector = injBusinessRegConnector
  val testBusinessRegConnector = injTestBusinessRegConnector
}

trait CurrentProfileCtrl extends FrontendController with Actions {

  val keystoreConnector: KeystoreConnector
  val businessRegConnector: BusinessRegistrationConnector
  val testBusinessRegConnector: TestBusinessRegConnector

  def currentProfileSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    businessRegConnector.retrieveCurrentProfile flatMap {
      case BusinessRegistrationSuccessResponse(profile) =>
        keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, profile).map {
          case x => Ok(s"Profile already set up for reg ID ${profile.registrationID}")
        }
      case _ =>
        testBusinessRegConnector.createCurrentProfileEntry flatMap { newProfile =>
          keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, newProfile).map {
            response => Ok(s"Profile set up for reg ID ${newProfile.registrationID}")
          }
        }
    }

  }

}
