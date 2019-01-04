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

import helpers.auth.AuthHelpers
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestCoHoControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val testHttpResponse = new HttpResponse {
    override def status = OK
  }

  class Setup extends CodeMocks with AuthHelpers {
    override val authConnector = mockAuthConnector
    override val keystoreConnector = mockKeystoreConnector


    val controller = new TestCoHoController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign

      override val testIncorpInfoConnector = mockTestIncorpInfoConnector
      override val keystoreConnector       = mockKeystoreConnector
      override val businessRegConnector    = mockBusinessRegistrationConnector
      override val coHoAPIService          = mockIncorpInfoService
      override val messagesApi             = mockMessagesApi
      override val authConnector           = mockAuthConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
    }
  }

  "coHoCompanyDetailsSetup" should {
    "return an OK" when {
      "the company details page has been rendered" in new Setup {
        showAuthorised(controller.coHoCompanyDetailsSetup, FakeRequest()) { result =>
          status(result) mustBe OK
        }
      }
    }
  }

  "submitCoHoCompanyDetailsSetup" should {
    "return an OK" when {
      "the registration ID has been fetched and the test company details submitted" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody(
          "companyName" -> "testCompanyName",
          "sicCodes[0]" -> "1234567890",
          "descriptions[0]" -> "testDescription-0",
          "sicCodes[1]" -> "9283745",
          "descriptions[1]" -> "testDescription-1",
          "sicCodes[2]" -> "",
          "descriptions[2]" -> "",
          "sicCodes[3]" -> "",
          "descriptions[3]" -> "",
          "sicCodes[4]" -> "",
          "descriptions[4]" -> ""
        )
        mockFetchCurrentProfile()

        mockBusinessRegFetch(Future(BusinessProfile(registrationID = "1", language = "EN")))

        when(mockTestIncorpInfoConnector.setupCoHoCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testHttpResponse))

        submitAuthorised(controller.submitCoHoCompanyDetailsSetup, request) {
          result =>
            status(result) mustBe OK
        }
      }
    }

    "return a BAD_REQUEST" when {
      "the form values are invalid" in new Setup {
        val request = FakeRequest().withFormUrlEncodedBody("invalidKey" -> "invalidValue")

        mockFetchCurrentProfile()

        submitAuthorised(controller.submitCoHoCompanyDetailsSetup, request) {
          result =>
            status(result) mustBe BAD_REQUEST
        }
      }
    }
  }

  "coHoCompanyDetailsTearDown" should {
    "return an OK" when {
      "the company details have been torn down" in new Setup {
        mockBusinessRegFetch(Future(BusinessProfile(registrationID = "1", language = "EN")))

        when(mockTestIncorpInfoConnector.teardownIndividualCoHoCompanyDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testHttpResponse))

        showAuthorised(controller.coHoCompanyDetailsTearDown, FakeRequest()) { result =>
          status(result) mustBe OK
        }
      }
    }
  }
}