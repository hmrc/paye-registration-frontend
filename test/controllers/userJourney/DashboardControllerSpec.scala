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

package controllers.userJourney

import config.AppConfig
import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import scala.concurrent.ExecutionContext

class DashboardControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val fakeRequest = FakeRequest("GET", "/")
  lazy val mockMcc = app.injector.instanceOf[MessagesControllerComponents]

  class Setup {
    val controller = new DashboardController(mockMcc) {
      override val redirectToLogin = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign = MockAuthRedirects.redirectToPostSign
      override val appConfig: AppConfig = mockAppConfig
      override val authConnector = mockAuthConnector
      override val keystoreConnector = mockKeystoreConnector
      override val companyRegUrl = "testUrl"
      override val companyRegUri = "/testUri"
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  "POST /dashboard" should {
    "return 303" in new Setup {
      AuthHelpers.showAuthorised(controller.dashboard, fakeRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(s"${controller.companyRegUrl}${controller.companyRegUri}/company-registration-overview")
      }
    }
  }
}
