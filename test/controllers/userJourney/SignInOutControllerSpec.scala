/*
 * Copyright 2020 HM Revenue & Customs
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

import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

class SignInOutControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val fakeRequest = FakeRequest()

  class Setup {
    val controller = new SignInOutController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val keystoreConnector = mockKeystoreConnector
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = mockMessagesApi
      override val compRegFEURL: String = "testUrl"
      override val compRegFEURI: String = "/testUri"
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
    }
  }

  "Calling the postSignIn action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.postSignIn, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "redirect the user to the Company Registration post-sign-in action" in new Setup {
      AuthHelpers.showAuthorised(controller.postSignIn, FakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/post-sign-in")
      }
    }
  }

  "signOut" should {
    "redirect to the exit questionnaire and clear the session" in new Setup {
      AuthHelpers.showAuthorised(controller.signOut, FakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(s"${controller.compRegFEURL}${controller.compRegFEURI}/questionnaire")
      }
    }
  }

  "renewSession" should {
    "return 200 when hit with Authorised User" in new Setup {
      AuthHelpers.showAuthorised(controller.renewSession(),FakeRequest()){a =>
        status(a) mustBe 200
        contentType(a) mustBe Some("image/jpeg")
        await(a).header.headers("Content-Disposition") mustBe """inline; filename="renewSession.jpg"; filename*=utf-8''renewSession.jpg"""
        await(a).body.toString.isEmpty mustBe false
      }
    }
  }

  "destroySession" should {
    "return redirect to timeout show and get rid of headers" in new Setup {

      val fr = FakeRequest().withHeaders(("playFoo","no more"))

      val res = controller.destroySession()(fr)
      status(res) mustBe 303
      headers(res).contains("playFoo") mustBe false

      redirectLocation(res) mustBe Some(controllers.userJourney.routes.SignInOutController.timeoutShow().url)
    }
  }

  "timeoutShow" should {
    "return 200" in new Setup {
      val res = controller.timeoutShow()(FakeRequest())
      status(res) mustBe 200
    }
  }
}
