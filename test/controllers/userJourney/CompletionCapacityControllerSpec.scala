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
import play.api.test.FakeRequest
import testHelpers.PAYERegSpec
import play.api.test.Helpers._

class CompletionCapacityControllerSpec extends PAYERegSpec {

  class Setup {
    val testController = new CompletionCapacityCtrl {
      override val authConnector = mockAuthConnector
      override val messagesApi = mockMessages
    }
  }

  "completionCapacity" should {
    "return an OK" in new Setup {
      AuthBuilder.showWithAuthorisedUser(testController.completionCapacity, mockAuthConnector) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "submitCompletionCapacity" should {
    "return a BadRequest" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )

      AuthBuilder.submitWithAuthorisedUser(testController.submitCompletionCapacity, mockAuthConnector, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a SEE_OTHER" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> ""
      )

      AuthBuilder.submitWithAuthorisedUser(testController.submitCompletionCapacity, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CompanyDetailsController.tradingName().url)
      }
    }
  }
}
