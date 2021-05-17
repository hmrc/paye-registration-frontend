/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.errors

import config.AppConfig
import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import scala.concurrent.ExecutionContext

class ErrorControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val regId = Fixtures.validCurrentProfile.get.registrationID
  val ticketId: Long = 123456789
  lazy val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  class Setup {
    val testController = new ErrorController(mockMcc) {
      override val redirectToLogin = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign = MockAuthRedirects.redirectToPostSign
      override val keystoreConnector = mockKeystoreConnector
      override val messagesApi = mockMessagesApi
      override val authConnector = mockAuthConnector
      override val thresholdService = mockThresholdService
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
      override implicit val appConfig: AppConfig = mockAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  "GET /start" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest("GET", "/authenticated/ineligible")

      AuthHelpers.showAuthorisedWithCP(testController.ineligible, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result) mustBe Some("utf-8")

      }
    }
  }

  "retrySubmission" should {
    "return 200" in new Setup {
      implicit val fakeRequest = FakeRequest("GET", "/")

      AuthHelpers.showAuthorisedWithCP(testController.retrySubmission, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result) mustBe Some("utf-8")
      }
    }
  }

  "failedSubmission" should {
    "return 200" in new Setup {
      implicit val fakeRequest = FakeRequest("GET", "/")

      AuthHelpers.showAuthorisedWithCP(testController.failedSubmission, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result) mustBe Some("utf-8")
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("submissionFailedReportAProblem").attr("id") mustBe "submissionFailedReportAProblem"
      }
    }
  }
}
