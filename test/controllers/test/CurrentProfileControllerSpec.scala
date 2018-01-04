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
import connectors.test.TestBusinessRegConnect
import fixtures.KeystoreFixture
import models.external.{BusinessProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.Helpers.OK
import testHelpers.PAYERegSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, NotFoundException }

class CurrentProfileControllerSpec extends PAYERegSpec with KeystoreFixture {

  val mockTestBusRegConnector = mock[TestBusinessRegConnect]

  val testProfile = BusinessProfile("testRegId", "testLang")

  class Setup {
    val controller = new BusinessProfileCtrl {
      override val businessRegConnector = mockBusinessRegistrationConnector
      override val keystoreConnector = mockKeystoreConnector
      override val testBusinessRegConnector = mockTestBusRegConnector
      override val authConnector = mockAuthConnector
    }
  }

  "currentProfileSetup" should {
    "return an OK" when {
      "the current profile has been returned and has been cached in keystore" in new Setup {
        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[HttpReads[BusinessProfile]]()))
          .thenReturn(Future.successful(testProfile))

        when(mockKeystoreConnector.cache[CurrentProfile](ArgumentMatchers.any(), ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.successful(blankCacheMap))

        AuthBuilder.showWithAuthorisedUser(controller.businessProfileSetup, mockAuthConnector) { result =>
          status(result) shouldBe OK
        }
      }

      "the current profile hasn't been found, but has then proceeded to create one and cache it in keystore" in new Setup {
        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[HttpReads[BusinessProfile]]()))
          .thenReturn(Future.failed(new NotFoundException("")))

        when(mockTestBusRegConnector.createBusinessProfileEntry(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testProfile))

        when(mockKeystoreConnector.cache[CurrentProfile](ArgumentMatchers.any(), ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.successful(blankCacheMap))

        AuthBuilder.showWithAuthorisedUser(controller.businessProfileSetup, mockAuthConnector) { result =>
          status(result) shouldBe OK
        }
      }
    }
  }
}
