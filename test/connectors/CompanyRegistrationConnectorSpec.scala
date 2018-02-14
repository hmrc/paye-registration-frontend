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

package connectors

import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.external.CompanyRegistrationProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class CompanyRegistrationConnectorSpec extends PayeComponentSpec {

  val testUrl = "testUrl"
  val testUri = "testUri"

  class Setup(stubbed: Boolean) {
    val testConnector = new CompanyRegistrationConnector {
      val companyRegistrationUri          = testUri
      val companyRegistrationUrl          = testUrl
      val stubUri                         = testUri
      val stubUrl                         = testUrl
      val http                            = mockWSHttp
      override val metricsService         = new MockMetrics
      override val featureSwitch          = mockFeatureSwitch
      override def useCompanyRegistration = stubbed
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
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future(profileJson))

      val result = await(testConnector.getCompanyRegistrationDetails("testRegId"))
      result mustBe CompanyRegistrationProfile(status, transactionId, ackRefStatusOpt)
    }

    "throw a bad request exception" in new Setup(false) {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
    }

    "throw any other exception" in new Setup(false) {
      when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("tstException")))

      intercept[RuntimeException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
    }

    "be stubbed" when {
      "returning a CompanyProfile" in new Setup(false) {
        when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future(profileJson))

        val result = await(testConnector.getCompanyRegistrationDetails("testRegId"))
        result mustBe CompanyRegistrationProfile(status, transactionId, ackRefStatusOpt)
      }

      "throwing a bad request exception" in new Setup(false) {
        when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new BadRequestException("tstException")))

        intercept[BadRequestException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
      }

      "throwing any other exception" in new Setup(false) {
        when(mockWSHttp.GET[JsObject](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new RuntimeException("tstException")))

        intercept[RuntimeException](await(testConnector.getCompanyRegistrationDetails("testRegId")))
      }
    }
  }
}
