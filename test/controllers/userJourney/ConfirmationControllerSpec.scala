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

import helpers.auth.AuthHelpers
import helpers.{PayeComponentSpec, PayeFakedApp}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import services.ConfirmationService

import scala.concurrent.Future

class ConfirmationControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockConfirmationService = mock[ConfirmationService]

  class Setup extends AuthHelpers {
    override val authConnector = mockAuthConnector
    override val keystoreConnector = mockKeystoreConnector

    val controller = new ConfirmationController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService        = mockIncorpInfoService
      override val companyDetailsService    = mockCompanyDetailsService
      override val s4LService               = mockS4LService
      override val authConnector            = mockAuthConnector
      override val keystoreConnector        = mockKeystoreConnector
      override val confirmationService      = mockConfirmationService
      implicit val messagesApi: MessagesApi = mockMessagesApi
    }
  }

  "showConfirmation" should {
    "display the confirmation page with an acknowledgement reference retrieved from backend" in new Setup {
      when(mockConfirmationService.getAcknowledgementReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("BRPY00000000001")))

      showAuthorisedWithCP(controller.showConfirmation, Fixtures.validCurrentProfile, FakeRequest()) {
        result =>
          status(result) mustBe OK
      }
    }

    "show an error page when there is no acknowledgement reference returned from the backend" in new Setup {
      when(mockConfirmationService.getAcknowledgementReference(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      showAuthorisedWithCP(controller.showConfirmation, Fixtures.validCurrentProfile, FakeRequest()) {
        result =>
          status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
