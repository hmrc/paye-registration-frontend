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

package controllers.userJourney

import helpers.{PayeComponentSpec, PayeFakedApp}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers

class WelcomeControllerSpec extends PayeComponentSpec with PayeFakedApp {

  class Setup {
    val controller = new WelcomeController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val thresholdService         = mockThresholdService
      override val incorpInfoService        = mockIncorpInfoService
      override val companyDetailsService    = mockCompanyDetailsService
      override val s4LService               = mockS4LService
      override val keystoreConnector        = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = mockMessagesApi
      override val authConnector            = mockAuthConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService  = mockPayeRegService
    }
  }

  "GET /start" should {
    "return 200" in new Setup {

      when(mockThresholdService.getCurrentThresholds)
        .thenReturn(Map("weekly" -> 1, "monthly" -> 1, "annually" -> 1))

      AuthHelpers.showAuthorised(controller.show, FakeRequest()) {
        result =>
          status(result)      mustBe Status.OK
          contentType(result) mustBe Some("text/html")
          charset(result)     mustBe Some("utf-8")
      }
    }
  }

  "POST /start" should {
    "return 303" in new Setup {
      AuthHelpers.showAuthorised(controller.submit, FakeRequest()) {
        result =>
          status(result)           mustBe Status.SEE_OTHER
          redirectLocation(result) mustBe Some(s"${controllers.userJourney.routes.EligibilityController.companyEligibility()}")
      }
    }
  }
}
