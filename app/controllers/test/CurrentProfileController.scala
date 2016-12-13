/*
 * Copyright 2016 HM Revenue & Customs
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
import connectors.{KeystoreConnector, BusinessRegistrationSuccessResponse, BusinessRegistrationConnector}
import enums.CacheKeys
import models.businessRegistration.BusinessRegistration
import models.currentProfile.CurrentProfile
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object CurrentProfileController extends CurrentProfileController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val keystoreConnector = KeystoreConnector
  //$COVERAGE-ON$
}

trait CurrentProfileController extends FrontendController with Actions {

  val keystoreConnector: KeystoreConnector

  def currentProfileSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    BusinessRegistrationConnector.retrieveCurrentProfile flatMap {
      case BusinessRegistrationSuccessResponse(profile) => {

        keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, createCurrentProfileFromBRResponse(profile)).map {
          case x => Ok(s"Profile already set up for reg ID ${profile.registrationID}")
        }
      }
      case _ => BusinessRegistrationConnector.createCurrentProfileEntry flatMap { brResponse =>
        keystoreConnector.cache[CurrentProfile](CacheKeys.CurrentProfile.toString, createCurrentProfileFromBRResponse(brResponse)).map {
          response => Ok(s"Profile set up for reg ID ${brResponse.registrationID}")
        }
      }
    }

  }

  private def createCurrentProfileFromBRResponse(resp: BusinessRegistration): CurrentProfile = {
    CurrentProfile(resp.registrationID, resp.completionCapacity, resp.language)
  }

}
