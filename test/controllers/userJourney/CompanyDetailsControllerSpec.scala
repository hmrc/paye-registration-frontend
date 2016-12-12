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

package controllers.userJourney

import builders.AuthBuilder
import helpers.PAYERegSpec
import play.api.http.Status
import play.api.test.FakeRequest

class CompanyDetailsControllerSpec extends PAYERegSpec {


  class Setup {
    val controller = new CompanyDetailsController {
      override val s4LConnector = mockS4LConnector
      override val keystoreConnector = mockKeystoreConnector
      override val authConnector = mockAuthConnector
    }
  }

  val fakeRequest = FakeRequest("GET", "/")


  "calling the tradingName action" should {
    "return 200 for an authorised user" in new Setup {
      AuthBuilder.showWithAuthorisedUser(controller.tradingName, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.OK
      }
    }

    "return 303 for an unauthorised user" in new Setup {
      val result = controller.tradingName()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "calling the submitTradingName action" should {
    "return 303 when a user enters valid data" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "tradeUnderDifferentName" -> "yes",
        "tradingName" -> "Tradez R us"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 400 when a user enters no data" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "return 400 when a user enters invalid data" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitTradingName(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "tradeUnderDifferentName" -> "yes"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

}
