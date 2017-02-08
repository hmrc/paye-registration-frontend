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

package controllers.userJourney

import builders.AuthBuilder
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import play.api.test.Helpers._

class RegisteredOfficeAddressControllerSpec extends PAYERegSpec {

  val mockMessages = injector.instanceOf[MessagesApi]

  class Setup {
    val testController = new RegisteredOfficeAddressCtrl {
      val authConnector = mockAuthConnector
      val messagesApi = mockMessages
    }

  }

  "roAddress" should {
    "return an ok" when {
      "the user is authorised to view the page" in new Setup {
        AuthBuilder.showWithAuthorisedUser(testController.roAddress, mockAuthConnector) {
          result =>
            status(result) shouldBe OK
        }
      }
    }

    "return a forbidden" when {
      "the user is not authorised to view the page" in new Setup {
        AuthBuilder.showWithAuthorisedUser(testController.confirmRO, mockAuthConnector) {
          result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"${controllers.userJourney.routes.AddressLookupController.redirectToLookup()}")
        }
      }
    }

    "return an internal server error" when {
      "there is a problem retrieving the users RO Address" ignore new Setup {

      }
    }
  }
}
