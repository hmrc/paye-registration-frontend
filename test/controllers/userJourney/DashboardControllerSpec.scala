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

import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest

class DashboardControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  class Setup {
    val controller: DashboardController = new DashboardController(
      keystoreConnector = mockKeystoreConnector,
      authConnector = mockAuthConnector,
      s4LService = mockS4LService,
      companyDetailsService = mockCompanyDetailsService,
      incorpInfoService = mockIncorpInfoService,
      incorporationInformationConnector = mockIncorpInfoConnector,
      payeRegistrationService = mockPayeRegService,
      mcc = mockMcc
    )(injAppConfig, ec) {
      override lazy val redirectToLogin: Result = MockAuthRedirects.redirectToLogin
      override lazy val redirectToPostSign: Result = MockAuthRedirects.redirectToPostSign
      override lazy val companyRegUrl = "testUrl"
      override lazy val companyRegUri = "/testUri"

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
