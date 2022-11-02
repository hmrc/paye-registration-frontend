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

import config.AppConfig
import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends PayeComponentSpec {

  class Setup {

    val mockAppConfig = mock[AppConfig]
    val mockServicesConfig = mock[ServicesConfig]

    when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl("business-registration")).thenReturn("testBusinessRegUrl")

    val testConnector = new BusinessRegistrationConnector(
      metricsService = new MockMetrics,
      http = mockHttpClient,
      appConfig = mockAppConfig
    )(scala.concurrent.ExecutionContext.Implicits.global)
  }

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      when(mockHttpClient.GET[BusinessProfile](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future(Fixtures.validBusinessRegistrationResponse))

      await(testConnector.retrieveCurrentProfile) mustBe Fixtures.validBusinessRegistrationResponse
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockHttpClient.GET[BusinessProfile](ArgumentMatchers.contains("/business-registration/business-tax-registration"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Runtime Exception")))

      intercept[RuntimeException](await(testConnector.retrieveCurrentProfile))
    }
  }

  "retrieveCompletionCapacity" should {
    "return an optional string when connector response is successful" in new Setup {
      when(mockHttpClient.GET[Option[String]](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("director")))

      await(testConnector.retrieveCompletionCapacity) mustBe Some("director")
    }

    "throw a Exception when something unexpected happened" in new Setup {
      when(mockHttpClient.GET[JsValue](ArgumentMatchers.contains("/business-registration/business-tax-registration"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("Run time exception")))

      intercept[RuntimeException](await(testConnector.retrieveCompletionCapacity))
    }
  }
  val tradingName = "tradingName is here and now 12345"
  "retrieveTradingName" should {
    val regId = "12345"
    "return an optional string Some(trading name)" in new Setup {
      when(mockHttpClient.GET[Option[String]](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(tradingName)))

      await(testConnector.retrieveTradingName(regId)) mustBe Some(tradingName)
    }
    "return None when unexpected exception occurs" in new Setup {
      when(mockHttpClient.GET[Option[String]](ArgumentMatchers.contains(s"/business-registration/$regId/trading-name"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

      await(testConnector.retrieveTradingName(regId)) mustBe None
    }
  }
  "upsertTradingName" should {
    val regId = "12345"
    "return the trading name on successful response from Business-Registration" in new Setup {
      when(mockHttpClient.POST[String, String](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(tradingName))

      await(testConnector.upsertTradingName(regId, tradingName)) mustBe tradingName

    }
    "return the trading name on a non success response from Business-Registration" in new Setup {
      when(mockHttpClient.POST[String, String](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo bar wizz bang")))

      await(testConnector.upsertTradingName(regId, tradingName)) mustBe tradingName
    }
  }

  "retrieveContactDetails" should {
    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return an optional PAYE Contact Details if contact details are found in Business Registration" in new Setup {
      when(mockHttpClient.GET[Option[PAYEContactDetails]](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validContactDetails)))

      await(testConnector.retrieveContactDetails(regId)) mustBe Some(validContactDetails)
    }

    "return no Contact Details if unexpected exception occurs" in new Setup {
      when(mockHttpClient.GET[Option[PAYEContactDetails]](ArgumentMatchers.contains(s"/business-registration/$regId/contact-details"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

      await(testConnector.retrieveContactDetails(regId)) mustBe None
    }
  }

  "upsertContactDetails" should {
    val regId = "12345"

    val validContactDetails = PAYEContactDetails("Test Name", DigitalContactDetails(Some("email@test.test"), Some("012345"), Some("543210")))

    "return PAYE Contact Details if contact details are stored in Business Registration" in new Setup {
      when(mockHttpClient.POST[PAYEContactDetails, PAYEContactDetails](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validContactDetails))

      await(testConnector.upsertContactDetails(regId, validContactDetails)) mustBe validContactDetails
    }

    "return Contact Details if contact details are not stored in Business Registration" in new Setup {
      when(mockHttpClient.POST[PAYEContactDetails, PAYEContactDetails](ArgumentMatchers.anyString(), ArgumentMatchers.any[PAYEContactDetails](), ArgumentMatchers.any())
        (ArgumentMatchers.any[Writes[PAYEContactDetails]](), ArgumentMatchers.any[HttpReads[PAYEContactDetails]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

      await(testConnector.upsertContactDetails(regId, validContactDetails)) mustBe validContactDetails
    }
  }

  "RetrieveAddresses" should {
    val regId = "54321"

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

    "return a list of addresses" in new Setup {
      when(mockHttpClient.GET[Seq[Address]](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(addresses))

      await(testConnector.retrieveAddresses(regId)) mustBe addresses
    }

    "return an empty list of addresses if unexpected exception occurs" in new Setup {
      when(mockHttpClient.GET[Seq[Address]](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"),ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

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
      when(mockHttpClient.POST[Address, Address](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[Address]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(address))

      await(testConnector.upsertAddress(regId, address)) mustBe address
    }

    "successfully complete in case of unexpected exception" in new Setup {
      when(mockHttpClient.POST[Address, Address](ArgumentMatchers.contains(s"/business-registration/$regId/addresses"), ArgumentMatchers.any[Address](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[Address]](), ArgumentMatchers.any[HttpReads[Address]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception("foo")))

      await(testConnector.upsertAddress(regId, address)) mustBe address
    }
  }
}
