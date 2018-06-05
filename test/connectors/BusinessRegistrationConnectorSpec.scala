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
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http._

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends PayeComponentSpec {

  class Setup {
    val testConnector = new BusinessRegistrationConnector {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http           = mockWSHttp
      override val metricsService = new MockMetrics
    }
  }

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.validBusinessRegistrationResponse))

      await(testConnector.retrieveCurrentProfile) mustBe Fixtures.validBusinessRegistrationResponse
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      intercept[NotFoundException](await(testConnector.retrieveCurrentProfile))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException](await(testConnector.retrieveCurrentProfile))
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Runtime Exception")))

      intercept[RuntimeException](await(testConnector.retrieveCurrentProfile))
    }
  }

  "retrieveCompletionCapacity" should {
    "return an optional string if CC is found in the BR document" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future(Json.parse(
          """
            |{
            | "completionCapacity" : "director"
            |}
          """.stripMargin)))

      await(testConnector.retrieveCompletionCapacity) mustBe Some("director")
    }

    "return none if the CC isn't in the BR document" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future(Json.parse("""{}""")))

      await(testConnector.retrieveCompletionCapacity) mustBe None
    }

    "return no CC if the Business Registration returns 404" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Not Found")))

      await(testConnector.retrieveCompletionCapacity) mustBe None

    }

    "throw a Forbidden exception if the request has been deemed unauthorised" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Forbidden", 403, 403)))

      intercept[Upstream4xxResponse](await(testConnector.retrieveCompletionCapacity))
    }

    "throw a Exception when something unexpected happened" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Run time exception")))

      intercept[RuntimeException](await(testConnector.retrieveCompletionCapacity))
    }
  }
  val tradingName = "tradingName is here and now 12345"
  val validTradingNameJson = Json.parse(
    s"""
       |{
       | "tradingName" : "$tradingName"
       |}
      """.stripMargin)

  val invalidTradingNameJson = Json.parse(
    """
      |{
      |  "tradingName": 1234567890
      |}
    """.stripMargin)

  "retrieveTradingName" should {
    val regId = "12345"
    "return an optional string Some(trading name)" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validTradingNameJson))

      await(testConnector.retrieveTradingName(regId)) mustBe Some(tradingName)
    }
    "return None when anything but a success response is returned" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

      await(testConnector.retrieveTradingName(regId)) mustBe None
    }
    "return None when success response is received but json is empty" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Json.obj()))

      await(testConnector.retrieveTradingName(regId)) mustBe None
    }
    "return None when success repsonse is received but json is invalid" in new Setup {
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(invalidTradingNameJson))

      await(testConnector.retrieveTradingName(regId)) mustBe None
    }
  }
  "upsertTradingName" should {
    val regId = "12345"
    "return the trading name on successful response from Business-Registration" in new Setup {
      when(mockWSHttp.POST[String, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(204)))

      await(testConnector.upsertTradingName(regId,tradingName)) mustBe tradingName

    }
    "return the trading name on a non success response from Business-Registration" in new Setup {
      when(mockWSHttp.POST[String, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo bar wizz bang")))

      await(testConnector.upsertTradingName(regId,tradingName)) mustBe tradingName
    }
  }

  "retrieveContactDetails" should {
    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return an optional PAYE Contact Details if contact details are found in Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validContactDetails))

      await(testConnector.retrieveContactDetails(regId)) mustBe Some(validContactDetails)
    }

    "return no Contact Details if contact details are not found in Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(testConnector.retrieveContactDetails(regId)) mustBe None
    }

    "return no Contact Details if bad request was made to Business Registration" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      await(testConnector.retrieveContactDetails(regId)) mustBe None
    }

    "return no Contact Details if Business Registration returns a 4xx" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("412", 412, 412)))

      await(testConnector.retrieveContactDetails(regId)) mustBe None
    }

    "return no Contact Details if Business Registration does not respond" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream5xxResponse("Timed out", 502, 502)))

      await(testConnector.retrieveContactDetails(regId)) mustBe None
    }
  }

  "upsertContactDetails" should {
    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return PAYE Contact Details if contact details are stored in Business Registration" in new Setup {
      when(mockWSHttp.POST[PAYEContactDetails, JsValue](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(Json.obj()))

      await(testConnector.upsertContactDetails(regId, validContactDetails)) mustBe validContactDetails
    }

    "return Contact Details if contact details are not stored in Business Registration" in new Setup {
      when(mockWSHttp.POST[PAYEContactDetails, JsValue](ArgumentMatchers.anyString(), ArgumentMatchers.any[PAYEContactDetails](), ArgumentMatchers.any())
        (ArgumentMatchers.any[Writes[PAYEContactDetails]](), ArgumentMatchers.any[HttpReads[JsValue]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(testConnector.upsertContactDetails(regId, validContactDetails)) mustBe validContactDetails
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
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(addressJson))

      await(testConnector.retrieveAddresses(regId)) mustBe addresses
    }

    "return an empty list of addresses in the case of an error" in new Setup{
      when(mockWSHttp.GET[JsValue](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("badRequest", 400, 400)))

      await(testConnector.retrieveAddresses(regId)) mustBe Seq.empty
    }

    "return an empty list of addresses if addresses are not found in BR & response code is 404" in new Setup {
      when(mockWSHttp.GET[PAYEContactDetails](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      await(testConnector.retrieveAddresses(regId)) mustBe Seq.empty
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
      when(mockWSHttp.POST[Address, HttpResponse](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[HttpResponse]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(200, None, Map.empty, None)))

      await(testConnector.upsertAddress(regId, address)) mustBe address
    }

    "successfully complete in case of BR error response" in new Setup {
      when(mockWSHttp.POST[Address, HttpResponse](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[HttpResponse]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream5xxResponse("error", 500, 500)))

      await(testConnector.upsertAddress(regId, address)) mustBe address
    }
  }
}
