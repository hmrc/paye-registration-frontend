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
import models.{Address, DigitalContactDetails}
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsString, Writes, JsValue, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http._

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
        .thenReturn(Future.failed(new Upstream4xxResponse("Forbidden", 403, 403)))

      intercept[Upstream4xxResponse](await(connector.retrieveCompletionCapacity))
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

    "return an optional PAYE Contact Details if contact details are found in Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validContactDetails))

      await(connector.retrieveContactDetails(regId)) shouldBe Some(validContactDetails)
    }

    "return no Contact Details if contact details are not found in Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(connector.retrieveContactDetails(regId)) shouldBe None
    }

    "return no Contact Details if bad request aws made to Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      await(connector.retrieveContactDetails(regId)) shouldBe None
    }

    "return no Contact Details if Business Registration returns a 4xx" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("412", 412, 412)))

      await(connector.retrieveContactDetails(regId)) shouldBe None
    }

    "return no Contact Details if Business Registration does not respond" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream5xxResponse("Timed out", 502, 502)))

      await(connector.retrieveContactDetails(regId)) shouldBe None
    }
  }

  "upsertContactDetails" should {
    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return PAYE Contact Details if contact details are stored in Business Registration" in new Setup {
      mockHttpPOST[PAYEContactDetails, JsValue]("testBusinessRegUrl/business-registration/$regId/contact-details", Json.obj())

      await(connector.upsertContactDetails(regId, validContactDetails)) shouldBe validContactDetails
    }

    "return Contact Details if contact details are not stored in Business Registration" in new Setup {
      when(mockWSHttp.POST[PAYEContactDetails, JsValue](ArgumentMatchers.anyString(), ArgumentMatchers.any[PAYEContactDetails](), ArgumentMatchers.any())
        (ArgumentMatchers.any[Writes[PAYEContactDetails]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(connector.upsertContactDetails(regId, validContactDetails)) shouldBe validContactDetails
    }
  }

  "RetrieveAddresses" should {
    val regId = "54321"

    val addressJson = Json.parse(
      """{
        |  "addresses":[
        |    {
        |      "addressLine1":"line 1",
        |      "addressLine2":"line 2",
        |      "addressLine3":"line 3",
        |      "country":"UK",
        |      "postcode":"TE1 1ST"
        |    },
        |    {
        |      "addressLine1":"line one",
        |      "addressLine2":"line two",
        |      "addressLine3":"line three",
        |      "addressLine4":"line four",
        |      "country":"UK"
        |    }
        |  ]
        |}
      """.stripMargin)

    val addresses = Seq(
      Address(
        "line 1",
        "line 2",
        Some("line 3"),
        None,
        Some("TE1 1ST"),
        None,
        None
      ),
      Address(
      "line one",
      "line two",
      Some("line three"),
      Some("line four"),
      None,
      Some("UK"),
      None
      )
    )

    "return a list of addresses" in new Setup{
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(addressJson))

      await(connector.retrieveAddresses(regId)) shouldBe addresses
    }

    "return an empty list of addresses in the case of an error" in new Setup{
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("badRequest", 400, 400)))

      await(connector.retrieveAddresses(regId)) shouldBe Seq.empty
    }
  }

  "UpsertAddress" should {
    val address = Address(
      "firstLine",
      "secondLine",
      None,
      None,
      Some("TE1 1ST")
    )
    val regId = "99999"
    "successfully upsert an address" in new Setup {
      when(mockWSHttp.POST[Address, JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(JsString("test")))

      await(connector.upsertAddress(regId, address)) shouldBe address
    }
    "successfully complete in case of BR error response" in new Setup {
      when(mockWSHttp.POST[Address, JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.failed(Upstream5xxResponse("error", 500, 500)))

      await(connector.upsertAddress(regId, address)) shouldBe address
    }
  }

  "RetrieveAddresses" should {
    val regId = "54321"

    val addressJson = Json.parse(
      """{
        |  "addresses":[
        |    {
        |      "addressLine1":"line 1",
        |      "addressLine2":"line 2",
        |      "addressLine3":"line 3",
        |      "country":"UK",
        |      "postcode":"TE1 1ST"
        |    },
        |    {
        |      "addressLine1":"line one",
        |      "addressLine2":"line two",
        |      "addressLine3":"line three",
        |      "addressLine4":"line four",
        |      "country":"UK"
        |    }
        |  ]
        |}
      """.stripMargin)

    val addresses = Seq(
      Address(
        "line 1",
        "line 2",
        Some("line 3"),
        None,
        Some("TE1 1ST"),
        None,
        None
      ),
      Address(
      "line one",
      "line two",
      Some("line three"),
      Some("line four"),
      None,
      Some("UK"),
      None
      )
    )

    "return a list of addresses" in new Setup{
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(addressJson))

      await(connector.retrieveAddresses(regId)) shouldBe addresses
    }

    "return an empty list of addresses in the case of an error" in new Setup{
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("badRequest", 400, 400)))

      await(connector.retrieveAddresses(regId)) shouldBe Seq.empty
    }
  }

  "UpsertAddress" should {
    val address = Address(
      "firstLine",
      "secondLine",
      None,
      None,
      Some("TE1 1ST")
    )
    val regId = "99999"
    "successfully upsert an address" in new Setup {
      when(mockWSHttp.POST[Address, JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(JsString("test")))

      await(connector.upsertAddress(regId, address)) shouldBe address
    }
    "successfully complete in case of BR error response" in new Setup {
      when(mockWSHttp.POST[Address, JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.failed(Upstream5xxResponse("error", 500, 500)))

      await(connector.upsertAddress(regId, address)) shouldBe address
    }
  }
}
