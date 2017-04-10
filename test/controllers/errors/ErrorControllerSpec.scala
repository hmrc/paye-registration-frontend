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

package controllers.errors

import builders.AuthBuilder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DeskproService
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class ErrorControllerSpec extends PAYERegSpec {

  val fakeRequest = FakeRequest("GET", "/")

  val mockDeskproService = mock[DeskproService]

  val regId = "12345"
  val ticketId : Long = 123456789

  class Setup {
    val controller = new ErrorCtrl {
      override val keystoreConnector = mockKeystoreConnector
      override val deskproService = mockDeskproService
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "GET /start" should {
    "return 200" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.ineligible, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.OK
      }
    }
    "return HTML" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.ineligible, mockAuthConnector) {
        (result: Future[Result]) =>
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
      }
    }
  }

  "retrySubmission" should {
    "return 200" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.retrySubmission, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
      }
    }
  }

  "failedSubmission" should {
    "return 200" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.failedSubmission, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
      }
    }
  }

  "submitTicket" should {
    "return 400 when an empty form is submitted" in new Setup {
      mockFetchCurrentProfile()
      val request = FakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )

      AuthBuilder.submitWithAuthorisedUser(controller.submitTicket, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return 400 when an invalid email is entered" in new Setup {
      mockFetchCurrentProfile()
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "Michael Mouse",
        "email" -> "************",
        "message" -> "I can't provide a good email address"
      )

      AuthBuilder.submitWithAuthorisedUser(controller.submitTicket, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }

    "return 303" in new Setup {
      mockFetchCurrentProfile()
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "Michael Mouse",
        "email" -> "mic@mou.biz",
        "message" -> "I can't provide a good email address"
      )

      when(mockDeskproService.submitTicket(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(ticketId))

      AuthBuilder.submitWithAuthorisedUser(controller.submitTicket, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-for-paye/ticket-submitted")
      }
    }
  }

  "submittedTicket" should {
    "return 200" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.submittedTicket, mockAuthConnector) {
        (result: Future[Result]) =>
          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
      }
    }
  }
}
