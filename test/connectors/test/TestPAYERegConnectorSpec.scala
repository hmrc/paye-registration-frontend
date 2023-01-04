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

package connectors.test

import enums.DownstreamOutcome
import helpers.PayeComponentSpec
import models.api.PAYERegistration
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class TestPAYERegConnectorSpec extends PayeComponentSpec {

  implicit val request: FakeRequest[_] = FakeRequest()

  class Setup extends CodeMocks {
    val connector = new TestPAYERegConnector {
      override val payeRegConnector = mockPAYERegConnector
      override val http: HttpClient = mockHttpClient
      override val payeRegUrl: String = "tst-url"
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  "Calling addPAYERegistration" should {
    "return a successful outcome for a successful add of PAYE Registration" in new Setup {
      when(mockHttpClient.POST[PAYERegistration, DownstreamOutcome.Value](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      await(connector.addPAYERegistration(Fixtures.validPAYERegistrationAPI)) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful add of PAYE Registration" in new Setup {
      when(mockHttpClient.POST[PAYERegistration, DownstreamOutcome.Value](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      await(connector.addPAYERegistration(Fixtures.validPAYERegistrationAPI)) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling addTestCompanyDetails" should {
    "return a successful outcome for a successful add of Company Details" in new Setup {
      mockFetchCurrentProfile("54321")

      when(mockPAYERegConnector.upsertCompanyDetails(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validCompanyDetailsAPI))

      await(connector.addTestCompanyDetails(Fixtures.validCompanyDetailsAPI, "54321")) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful add of Company Details" in new Setup {
      mockFetchCurrentProfile("54321")

      when(mockPAYERegConnector.upsertCompanyDetails(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("tst")))

      await(connector.addTestCompanyDetails(Fixtures.validCompanyDetailsAPI, "54321")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling addTestPAYEContact" should {
    "return a successful outcome for a successful add of PAYE Contact" in new Setup {
      mockFetchCurrentProfile("54321")

      when(mockPAYERegConnector.upsertPAYEContact(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validPAYEContactAPI))

      await(connector.addTestPAYEContact(Fixtures.validPAYEContactAPI, "54321")) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful add of PAYE Contact" in new Setup {
      mockFetchCurrentProfile("54321")

      when(mockPAYERegConnector.upsertPAYEContact(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("tst")))

      await(connector.addTestPAYEContact(Fixtures.validPAYEContactAPI, "54321")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling testRegistrationTeardown" should {
    "return a successful outcome for a successful teardown" in new Setup {
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      await(connector.testRegistrationTeardown()) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful teardown" in new Setup {
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException))

      await(connector.testRegistrationTeardown()) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling tearDownIndividualRegistration" should {
    "return a successful outcome for a successful teardown" in new Setup {
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      await(connector.tearDownIndividualRegistration("regId")) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful teardown" in new Setup {
      when(mockHttpClient.GET[HttpResponse](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException))

      await(connector.tearDownIndividualRegistration("regId")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling update-status" should {
    "return a successful outcome for a successful update" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      await(connector.updateStatus("regId", "rejected")) mustBe DownstreamOutcome.Success
    }

    "return a failed outcome for an unsuccessful teardown" in new Setup {
      when(mockHttpClient.POST[JsObject, HttpResponse](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException))

      await(connector.updateStatus("regId", "submitted")) mustBe DownstreamOutcome.Failure
    }
  }
}
