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
import enums.{DownstreamOutcome, UserCapacity}
import models.view.CompletionCapacity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CompletionCapacityService
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CompletionCapacityControllerSpec extends PAYERegSpec {

  val mockCompletionCapacityService = mock[CompletionCapacityService]

  class Setup {
    val testController = new CompletionCapacityCtrl {
      override val authConnector = mockAuthConnector
      override val messagesApi = mockMessages
      override val completionCapacityService = mockCompletionCapacityService
      override val keystoreConnector = mockKeystoreConnector
    }
  }
  "completionCapacity" should {
    "return an OK if a capacity has been found" in new Setup {
      val capacity = CompletionCapacity(UserCapacity.director, "")

      mockFetchCurrentProfile()

      when(mockCompletionCapacityService.getCompletionCapacity(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(capacity)))

      AuthBuilder.showWithAuthorisedUser(testController.completionCapacity, mockAuthConnector) { result =>
        status(result) shouldBe OK
      }
    }

    "return an OK if a capacity has not been found" in new Setup {
      mockFetchCurrentProfile()
      when(mockCompletionCapacityService.getCompletionCapacity(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      AuthBuilder.showWithAuthorisedUser(testController.completionCapacity, mockAuthConnector) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "submitCompletionCapacity" should {
    "return a BadRequest" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )

      mockFetchCurrentProfile()
      AuthBuilder.submitWithAuthorisedUser(testController.submitCompletionCapacity, mockAuthConnector, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a SEE_OTHER" in new Setup {
      val capacity = CompletionCapacity(UserCapacity.director, "")

      val request = FakeRequest().withFormUrlEncodedBody(
        "completionCapacity" -> "director",
        "completionCapacityOther" -> ""
      )
      mockFetchCurrentProfile()

      when(mockCompletionCapacityService.saveCompletionCapacity(ArgumentMatchers.any[CompletionCapacity](), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      AuthBuilder.submitWithAuthorisedUser(testController.submitCompletionCapacity, mockAuthConnector, request) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CompanyDetailsController.tradingName().url)
      }
    }
  }
}
