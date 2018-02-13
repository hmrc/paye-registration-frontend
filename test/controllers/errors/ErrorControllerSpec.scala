/*
 * Copyright 2018 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.test.FakeRequest

import scala.concurrent.Future

class ErrorControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val regId = Fixtures.validCurrentProfile.get.registrationID
  val ticketId : Long = 123456789

  class Setup {
    val testController = new ErrorController {
      override val redirectToLogin        = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign     = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService      = mockIncorpInfoService
      override val companyDetailsService  = mockCompanyDetailsService
      override val s4LService             = mockS4LService
      override val keystoreConnector      = mockKeystoreConnector
      override val deskproService         = mockDeskproService
      override val messagesApi            = mockMessagesApi
      override val authConnector          = mockAuthConnector
    }
  }

  "GET /start" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest("GET", "/authenticated/ineligible")

      AuthHelpers.showAuthorisedWithCP(testController.ineligible, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result)      mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result)     mustBe Some("utf-8")
      }
    }
  }

  "retrySubmission" should {
    "return 200" in new Setup {
      implicit val fakeRequest = FakeRequest("GET", "/")

      AuthHelpers.showAuthorisedWithCP(testController.retrySubmission, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result)      mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result)     mustBe Some("utf-8")
      }
    }
  }

  "failedSubmission" should {
    "return 200" in new Setup {
      implicit val fakeRequest = FakeRequest("GET", "/")

      AuthHelpers.showAuthorisedWithCP(testController.failedSubmission, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result)      mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result)     mustBe Some("utf-8")
      }
    }
  }

  "submitTicket" should {
    "return 400 when an empty form is submitted" in new Setup {
      implicit val fakeRequest = FakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitTicket, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 400 when an invalid email is entered" in new Setup {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody(
        "name"    -> "Michael Mouse",
        "email"   -> "************",
        "message" -> "I can't provide a good email address"
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitTicket, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 303" in new Setup {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody(
        "name"    -> "Michael Mouse",
        "email"   -> "mic@mou.biz",
        "message" -> "I can't provide a good email address"
      )

      when(testController.deskproService.submitTicket(ArgumentMatchers.eq(regId), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(ticketId))

      AuthHelpers.submitAuthorisedWithCP(testController.submitTicket, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result)            mustBe SEE_OTHER
        redirectLocation(result)  mustBe Some("/register-for-paye/ticket-submitted")
      }
    }
  }

  "submittedTicket" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest()

      AuthHelpers.showAuthorisedWithCP(testController.submittedTicket, Fixtures.validCurrentProfile, fakeRequest) { result =>
        status(result)      mustBe OK
        contentType(result) mustBe Some("text/html")
        charset(result)     mustBe Some("utf-8")
      }
    }
  }
}
