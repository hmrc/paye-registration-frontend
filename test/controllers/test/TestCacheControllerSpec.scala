/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.test

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestCacheControllerSpec extends PayeComponentSpec with PayeFakedApp {
  val testHttpResponse = new HttpResponse {}

  class Setup extends CodeMocks {
    val controller = new TestCacheController {
      override val redirectToLogin        = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign     = MockAuthRedirects.redirectToPostSign

      override val keystoreConnector      = mockKeystoreConnector
      override val s4LService             = mockS4LService
      override val businessRegConnector   = mockBusinessRegistrationConnector
      override val messagesApi            = mockMessagesApi
      override val authConnector          = mockAuthConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
    }
  }

  "tearDownS4L" should {
    "return an OK" when {
      "Save4Later has been cleared" in new Setup {

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        mockBusinessRegFetch(Future(BusinessProfile(registrationID = "1", language = "EN")))

        when(mockS4LService.clear(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testHttpResponse))

        val result = controller.tearDownS4L()(FakeRequest())
        status(result) mustBe OK
      }
    }
  }
}