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

  class Setup(stubbed: Boolean) {

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
      override def useCompanyRegistration = stubbed
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  val status = "submitted"
  val transactionId = "submitted"
  val ackRefStatus = "04"
  val ackRefStatusOpt = Some(ackRefStatus)

  val profileJson =
    Json.parse(
      s"""
         |{
         |    "registration-id" : "testRegId",
         |    "status" : "$status",
         |    "confirmationReferences" : {
         |       "acknowledgement-reference" : "BRCT-0123456789",
         |       "transaction-id" : "$transactionId"
         |    },
         |    "acknowledgementReferences" : {
         |       "status" : "$ackRefStatus"
         |    }
         |}
      """.stripMargin).as[JsObject]

  val profileJsonMin =
    Json.parse(
      s"""
         |{
         |    "registration-id" : "testRegId",
         |    "status" : "$status"
         |}
      """.stripMargin).as[JsObject]

  "getCompanyRegistrationDetails" should {
    "return a CompanyProfile" in new Setup(false) {
      when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future(profileJson))

      val result = await(testConnector.getCompanyRegistrationDetails("testRegId"))
      result mustBe CompanyRegistrationProfile(status, transactionId, ackRefStatusOpt)
    }

    "throw a bad request exception" in new Setup(false) {
      when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
    }

    "throw any other exception" in new Setup(false) {
      when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.failed(new RuntimeException("tstException")))

      intercept[RuntimeException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
    }

    "be stubbed" when {
      "returning a CompanyProfile" in new Setup(false) {
        when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
          .thenReturn(Future(profileJson))

        val result = await(testConnector.getCompanyRegistrationDetails("testRegId"))
        result mustBe CompanyRegistrationProfile(status, transactionId, ackRefStatusOpt)
      }

      "throwing a bad request exception" in new Setup(false) {
        when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
          .thenReturn(Future.failed(new BadRequestException("tstException")))

        intercept[BadRequestException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
      }

      "throwing any other exception" in new Setup(false) {
        when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
          .thenReturn(Future.failed(new RuntimeException("tstException")))

        intercept[RuntimeException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
      }
    }
  }
  "getVerifiedEmail" should {
    val emailResponse = Json.parse(
      """{
        | "address": "foo@foo.com",
        | "type": "foo",
        | "link-sent": true,
        | "verified": true,
        | "return-link-email-sent": true
        |}
      """.stripMargin).as[JsObject]
    "return future option string" in new Setup(stubbed = false) {
      when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.successful(emailResponse))

      val res = await(testConnector.getVerifiedEmail("fooBarAndWizz"))
      res mustBe Some("foo@foo.com")
      verify(mockHttpClient, times(1)).GET[JsObject](any(),any(),any())(any(), any(), any())
    }
    "return a None when company reg call fails" in new Setup(stubbed = false) {
      when(mockHttpClient.GET[JsObject](any(),any(),any())(any(), any[HeaderCarrier](), any()))
        .thenReturn(Future.failed(new BadRequestException("tstException")))

      val res = await(testConnector.getVerifiedEmail("fooBarAndWizz"))
      res mustBe None
    }
  }
}
