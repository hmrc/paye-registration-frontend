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

import fixtures.BusinessRegistrationFixture
import mocks.MockMetrics
import models.DigitalContactDetails
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends PAYERegSpec with BusinessRegistrationFixture {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]

  trait Setup {
    val connector = new BusinessRegistrationConnect {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http = mockWSHttp
      override val metricsService = new MockMetrics
    }
  }

  implicit val hc = HeaderCarrier()

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      mockHttpGet[BusinessProfile]("testUrl", validBusinessRegistrationResponse)

      await(connector.retrieveCurrentProfile) shouldBe validBusinessRegistrationResponse
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      intercept[NotFoundException](await(connector.retrieveCurrentProfile))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException](await(connector.retrieveCurrentProfile))
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Runtime Exception")))

      intercept[RuntimeException](await(connector.retrieveCurrentProfile))
    }
  }

  "retrieveCompletionCapacity" should {
    "return an optional string if CC is found in the BR document" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Json.parse(
          """
            |{
            | "completionCapacity" : "director"
            |}
          """.stripMargin)))

      await(connector.retrieveCompletionCapacity) shouldBe Some("director")
    }

    "return none if the CC isn't in the BR document" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Json.parse("""{}""")))

      await(connector.retrieveCompletionCapacity) shouldBe None
    }

    "throw a NotFoundException if the response code is a 404" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      intercept[NotFoundException](await(connector.retrieveCompletionCapacity))
    }

    "throw a Forbidden exception if the request has been deemed unauthorised" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException](await(connector.retrieveCompletionCapacity))
    }

    "throw a Exception when something unexpected happened" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Run time exception")))

      intercept[RuntimeException](await(connector.retrieveCompletionCapacity))
    }
  }

  "retrieveContactDetails" should {

    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return an optional string if contact details are found in the BR document" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validContactDetails))

      await(connector.retrieveContactDetails(regId)) shouldBe validContactDetails
    }
  }
}
0