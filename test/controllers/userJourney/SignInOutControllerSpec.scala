/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import views.html.templates.timeout

class SignInOutControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val fakeRequest = FakeRequest()

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockTimeout: timeout = app.injector.instanceOf[timeout]
  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  class Setup {
    val controller = new SignInOutController(
      mockAuthConnector,
      mockS4LService,
      mockCompanyDetailsService,
      mockIncorpInfoService,
      mockKeystoreConnector,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockTimeout
    ) {
      override lazy val compRegFEURL: String = "testUrl"
      override lazy val compRegFEURI: String = "/testUri"
    }
  }

  "Calling the postSignIn action" should {
    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showUnauthorised(controller.postSignIn, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
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
          redirectLocation(result) mustBe Some(appConfig.betaFeedbackUrl)
      }
    }
  }

  "renewSession" should {
    "return 200 when hit with Authorised User" in new Setup {
      AuthHelpers.showAuthorised(controller.renewSession(), FakeRequest()) { a =>
        status(a) mustBe 200
        contentType(a) mustBe Some("image/jpeg")
        await(a).header.headers("Content-Disposition") mustBe """inline; filename="renewSession.jpg""""
        await(a).body.toString.isEmpty mustBe false
      }
    }
  }

  "destroySession" should {
    "return redirect to timeout show and get rid of headers" in new Setup {

      val fr = FakeRequest().withHeaders(("playFoo", "no more"))

      val res = controller.destroySession()(fr)
      status(res) mustBe 303
      headers(res).contains("playFoo") mustBe false

      redirectLocation(res) mustBe Some(controllers.userJourney.routes.SignInOutController.timeoutShow.url)
    }
  }

  "timeoutShow" should {
    "return 200" in new Setup {
      val res = controller.timeoutShow()(FakeRequest())
      status(res) mustBe 200
    }
  }
}
