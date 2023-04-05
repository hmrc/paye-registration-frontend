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

package controllers.test

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.{BusinessProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class CurrentProfileControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val testProfile: BusinessProfile = BusinessProfile("testRegId", "testLang")
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  class Setup {
    val controller: BusinessProfileController = new BusinessProfileController(
      keystoreConnector = mockKeystoreConnector,
      businessRegConnector = mockBusinessRegistrationConnector,
      authConnector = mockAuthConnector,
      s4LService = mockS4LService,
      companyDetailsService = mockCompanyDetailsService,
      incorpInfoService = mockIncorpInfoService,
      testBusinessRegConnector = mockTestBusRegConnector,
      incorporationInformationConnector = mockIncorpInfoConnector,
      payeRegistrationService = mockPayeRegService,
      mcc = mockMcc
    )(injAppConfig, ec) {
      override lazy val redirectToLogin: Result = MockAuthRedirects.redirectToLogin
      override lazy val redirectToPostSign: Result = MockAuthRedirects.redirectToPostSign
    }
  }

  "currentProfileSetup" should {
    "return an OK" when {
      "the current profile has been returned and has been cached in keystore" in new Setup {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.successful(testProfile))

        when(mockKeystoreConnector.cache[CurrentProfile](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.blankSessionMap))

        AuthHelpers.showAuthorised(controller.businessProfileSetup, request) { result =>
          status(result) mustBe OK
        }
      }

      "the current profile hasn't been found, but has then proceeded to create one and cache it in keystore" in new Setup {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

        when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new NotFoundException("")))

        when(mockTestBusRegConnector.createBusinessProfileEntry(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.successful(testProfile))

        when(mockKeystoreConnector.cache[CurrentProfile](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[CurrentProfile]())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.blankSessionMap))

        AuthHelpers.showAuthorised(controller.businessProfileSetup, request) { result =>
          status(result) mustBe OK
        }
      }
    }
  }
}