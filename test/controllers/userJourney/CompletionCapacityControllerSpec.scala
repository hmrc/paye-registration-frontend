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

import enums.{DownstreamOutcome, UserCapacity}
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.CompletionCapacity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import services.CompletionCapacityService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CompletionCapacityControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val mockCompletionCapacityService = mock[CompletionCapacityService]

  class Setup {
    val testController = new CompletionCapacityController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val incorpInfoService         = mockIncorpInfoService
      override val companyDetailsService     = mockCompanyDetailsService
      override val s4LService                = mockS4LService
      override val authConnector             = mockAuthConnector
      override val messagesApi               = mockMessagesApi
      override val completionCapacityService = mockCompletionCapacityService
      override val keystoreConnector         = mockKeystoreConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService   = mockPayeRegService
    }
  }
  "completionCapacity" should {
    "return an OK if a capacity has been found" in new Setup {
      val capacity = CompletionCapacity(UserCapacity.director, "")

      when(mockCompletionCapacityService.getCompletionCapacity(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(capacity)))

      AuthHelpers.showAuthorisedWithCP(testController.completionCapacity, Fixtures.validCurrentProfile, FakeRequest()) { result =>
        status(result) mustBe OK
      }
    }

    "return an OK if a capacity has NOT been found" in new Setup {
      val capacity = CompletionCapacity(UserCapacity.director, "")

      when(mockCompletionCapacityService.getCompletionCapacity(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showAuthorisedWithCP(testController.completionCapacity, Fixtures.validCurrentProfile, FakeRequest()) { result =>
        status(result) mustBe OK
      }
    }
  }

  "submitCompletionCapacity" should {
    "return a BadRequest" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )

      AuthHelpers.submitAuthorisedWithCP(testController.submitCompletionCapacity, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "return a SEE_OTHER" in new Setup {
      val capacity = CompletionCapacity(UserCapacity.director, "")

      val request = FakeRequest().withFormUrlEncodedBody(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> ""
      )

      when(mockCompletionCapacityService.saveCompletionCapacity(ArgumentMatchers.anyString(), ArgumentMatchers.any[CompletionCapacity]())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthHelpers.submitAuthorisedWithCP(testController.submitCompletionCapacity, Fixtures.validCurrentProfile, request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CompanyDetailsController.tradingName().url)
      }
    }
  }
}
