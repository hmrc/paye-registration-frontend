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

package connectors

import mocks.MockMetrics
import models.external.CompanyRegistrationProfile
import play.api.libs.json.{JsObject, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import utils.PAYEFeatureSwitch

import scala.concurrent.Future

class CompanyRegistrationConnectorSpec extends PAYERegSpec {

  val testUrl = "testUrl"
  val testUri = "testUri"
  val mockFeatureSwitch = mock[PAYEFeatureSwitch]

  class Setup(stubbed: Boolean) {
    val connector = new CompanyRegistrationConnect {
      val companyRegistrationUri = testUri
      val companyRegistrationUrl = testUrl
      val stubUri = testUri
      val stubUrl = testUrl
      val http = mockWSHttp
      override val metricsService = new MockMetrics
      override val featureSwitch = mockFeatureSwitch
      override def useCompanyRegistration = stubbed
    }

    implicit val hc = HeaderCarrier()
  }

  val status = "submitted"
  val transactionId = "submitted"

  val profileJson =
    Json.parse(
      s"""
        |{
        |    "registration-id" : "testRegId",
        |    "status" : "$status",
        |    "confirmationReferences" : {
        |       "acknowledgement-reference" : "BRCT-0123456789",
        |       "transaction-id" : "$transactionId"
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
      mockHttpGet[JsObject](connector.companyRegistrationUri, Future.successful(profileJson))

      val result = await(connector.getCompanyRegistrationDetails("testRegId"))
      result shouldBe CompanyRegistrationProfile(status, transactionId)
    }

    "throw a bad request exception" in new Setup(false) {
      mockHttpGet[JsObject](connector.companyRegistrationUri, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getCompanyRegistrationDetails("testRegId")))
    }

    "throw any other exception" in new Setup(false) {
      mockHttpGet[JsObject](connector.companyRegistrationUri, Future.failed(new RuntimeException))

      intercept[RuntimeException](await(connector.getCompanyRegistrationDetails("testRegId")))
    }

    "be stubbed" when {
      "returning a CompanyProfile" in new Setup(false) {
        mockHttpGet[JsObject](connector.companyRegistrationUri, Future.successful(profileJson))

        val result = await(connector.getCompanyRegistrationDetails("testRegId"))
        result shouldBe CompanyRegistrationProfile(status, transactionId)
      }

      "throwing a bad request exception" in new Setup(false) {
        mockHttpGet[JsObject](connector.companyRegistrationUri, Future.failed(new BadRequestException("tstException")))

        intercept[BadRequestException](await(connector.getCompanyRegistrationDetails("testRegId")))
      }

      "throwing any other exception" in new Setup(false) {
        mockHttpGet[JsObject](connector.companyRegistrationUri, Future.failed(new RuntimeException))

        intercept[RuntimeException](await(connector.getCompanyRegistrationDetails("testRegId")))
      }
    }
  }
}
