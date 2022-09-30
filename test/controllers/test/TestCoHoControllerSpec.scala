/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.KeystoreConnector
import helpers.auth.AuthHelpers
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import views.html.pages.test.coHoCompanyDetailsSetup

import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.Future

class TestCoHoControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val testHttpResponse = HttpResponse(status = OK, body = "")

  class Setup extends CodeMocks with AuthHelpers {
    override val authConnector: AuthConnector = mockAuthConnector
    override val keystoreConnector: KeystoreConnector = mockKeystoreConnector

    lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
    lazy val mockView: coHoCompanyDetailsSetup = app.injector.instanceOf[coHoCompanyDetailsSetup]

    val controller = new TestCoHoController(
      mockTestIncorpInfoConnector,
      mockIncorpInfoService,
      mockKeystoreConnector,
      mockBusinessRegistrationConnector,
      mockAuthConnector,
      mockS4LService,
      mockCompanyDetailsService,
      mockIncorpInfoService,
      mockIncorpInfoConnector,
      mockPayeRegService,
      mockMcc,
      mockView
    )(mockAppConfig,
      globalExecutionContext
    )
  }

  "coHoCompanyDetailsSetup" should {
    "return an OK" when {
      "the company details page has been rendered" in new Setup {
        showAuthorised(controller.coHoCompanyDetailsSetup, fakeRequest()) { result =>
          status(result) mustBe OK
        }
      }
    }
  }

  "submitCoHoCompanyDetailsSetup" should {
    "return an OK" when {
      "the registration ID has been fetched and the test company details submitted" in new Setup {
        val request = fakeRequest("POST").withFormUrlEncodedBody(
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

        mockBusinessRegFetch(Future.successful(BusinessProfile(registrationID = "1", language = "EN")))

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
        val request = fakeRequest("POST").withFormUrlEncodedBody("invalidKey" -> "invalidValue")

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
        mockBusinessRegFetch(Future.successful(BusinessProfile(registrationID = "1", language = "EN")))

        when(mockTestIncorpInfoConnector.teardownIndividualCoHoCompanyDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testHttpResponse))

        showAuthorised(controller.coHoCompanyDetailsTearDown, fakeRequest()) { result =>
          status(result) mustBe OK
        }
      }
    }
  }
}