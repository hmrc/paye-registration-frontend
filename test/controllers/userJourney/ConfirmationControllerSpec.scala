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

package controllers.userJourney

import builders.AuthBuilder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.http.Status.OK
import play.api.i18n.MessagesApi
import services.ConfirmationService
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class ConfirmationControllerSpec extends PAYERegSpec {

  val mockConfirmationService = mock[ConfirmationService]

  class Setup {
    val controller = new ConfirmationCtrl {
      override val authConnector = mockAuthConnector
      override val keystoreConnector = mockKeystoreConnector
      override val confirmationService = mockConfirmationService
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "showConfirmation" should {
    "display the confirmation page with an acknowledgement reference retrieved from backend" in new Setup {
      mockFetchCurrentProfile()

      when(mockConfirmationService.getAcknowledgementReference(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("BRPY00000000001")))

      AuthBuilder.showWithAuthorisedUser(controller.showConfirmation, mockAuthConnector) {
        result =>
          status(result) shouldBe OK
      }
    }

    "show an error page when there is no acknowledgement reference returned from the backend" in new Setup {
      mockFetchCurrentProfile()

      when(mockConfirmationService.getAcknowledgementReference(ArgumentMatchers.contains("12345"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      AuthBuilder.showWithAuthorisedUser(controller.showConfirmation, mockAuthConnector) {
        result =>
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

}
