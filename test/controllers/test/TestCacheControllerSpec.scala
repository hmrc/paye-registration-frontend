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

package controllers.test

import builders.AuthBuilder
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.test.Helpers.OK
import services.S4LSrv
import testHelpers.PAYERegSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class TestCacheControllerSpec extends PAYERegSpec {

  val mockS4LService = mock[S4LSrv]

  val testHttpResponse = new HttpResponse {}

  class Setup {
    val controller = new TestCacheCtrl {
      override val s4LService = mockS4LService
      override val businessRegConnector = mockBusinessRegistrationConnector
      override val messagesApi = mockMessages
      override val authConnector = mockAuthConnector
    }
  }

  "tearDownS4L" should {
    "return an OK" when {
      "Save4Later has been cleared" in new Setup {
        mockFetchCurrentProfile()
        mockBusinessRegFetch(
          BusinessProfile(registrationID = "1",
                          language = "EN")
        )

        when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testHttpResponse))

        AuthBuilder.showWithAuthorisedUser(controller.tearDownS4L, mockAuthConnector) { result =>
          status(result) shouldBe OK
        }
      }
    }
  }
}
