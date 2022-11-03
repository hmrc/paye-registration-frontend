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

package connectors

import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.external.CompanyRegistrationProfile
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class CompanyRegistrationConnectorSpec extends PayeComponentSpec {

  val testUrl = "testUrl"
  val testUri = "testUri"

  class Setup {

    when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl("company-registration")).thenReturn(testUrl)
    when(mockServicesConfig.getString("microservice.services.company-registration.uri")).thenReturn(testUri)
    when(mockServicesConfig.baseUrl("incorporation-frontend-stubs")).thenReturn(testUrl)
    when(mockServicesConfig.getString("microservice.services.incorporation-frontend-stubs.uri")).thenReturn(testUri)

    val testConnector = new CompanyRegistrationConnector(
      mockFeatureSwitch,
      mockHttpClient,
      new MockMetrics,
      mockAppConfig
    ) {
      override def useCompanyRegistration = false
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  val status = "submitted"
  val transactionId = "submitted"
  val ackRefStatus = Some("04")

  "calling .getCompanyRegistrationDetails(regId: String)" should {

    "return a CompanyProfile when successful" in new Setup {

      val compRegProfile = CompanyRegistrationProfile(status, transactionId, ackRefStatus)

      when(mockHttpClient.GET[CompanyRegistrationProfile](any(), any(), any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future(compRegProfile))

      val result = await(testConnector.getCompanyRegistrationDetails("testRegId"))

      result mustBe compRegProfile
    }

    "throwing any other exception" in new Setup {

      when(mockHttpClient.GET[JsObject](any(), any(), any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.failed(new Exception("foo")))

      intercept[Exception](await(testConnector.getCompanyRegistrationDetails("testRegId")))
    }
  }

  "calling .getVerifiedEmail(regId: String)" should {

    "return future option string" in new Setup {

      val email = "foo@foo.com"

      when(mockHttpClient.GET[Option[String]](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.successful(Some(email)))

      val res = await(testConnector.getVerifiedEmail("fooBarAndWizz"))

      res mustBe Some(email)
    }

    "return a None for any other exception" in new Setup {

      when(mockHttpClient.GET[Option[String]](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.failed(new Exception("foo")))

      val res = await(testConnector.getVerifiedEmail("fooBarAndWizz"))

      res mustBe None
    }
  }
}

