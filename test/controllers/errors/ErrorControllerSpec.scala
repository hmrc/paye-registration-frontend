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

import helpers.{PayeComponentSpec, PayeFakedApp}
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import views.html.pages.error.{ineligible, newIneligible, submissionFailed, submissionTimeout}

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}

class ErrorControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val regId: String = Fixtures.validCurrentProfile.get.registrationID
  val ticketId: Long = 123456789
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockIneligibleView: ineligible = app.injector.instanceOf[ineligible]
  lazy val mockNewIneligibleView: newIneligible = app.injector.instanceOf[newIneligible]
  lazy val mockSubmissionTimeoutView: submissionTimeout = app.injector.instanceOf[submissionTimeout]
  lazy val mockSubmissionFailedView: submissionFailed = app.injector.instanceOf[submissionFailed]

  class Setup {

    val testController = new ErrorController(
      mockThresholdService,
      mockKeystoreConnector,
      mockCompanyDetailsService,
      mockS4LService,
      mockIncorpInfoService,
      mockAuthConnector,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockIneligibleView,
      mockNewIneligibleView,
      mockSubmissionTimeoutView,
      mockSubmissionFailedView
    )(mockAppConfig,
      globalExecutionContext)

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
