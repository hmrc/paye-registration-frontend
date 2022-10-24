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

import com.github.tomakehurst.wiremock.client.WireMock._
import common.exceptions.DownstreamExceptions
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external._
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class BusinessRegistrationConnectorISpec extends IntegrationSpecBase {

  implicit val hc = HeaderCarrier()

  val regId = "12345"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map("microservice.services.business-registration.port" -> s"${WiremockHelper.wiremockPort}"))
    .build

  lazy val connector = app.injector.instanceOf[BusinessRegistrationConnector]

  "BusinessRegistrationConnector" when {

    "calling .retrieveCurrentProfile()" must {

      val businessProfile = BusinessProfile("12345", "EN")
      val businessProfileJson = Json.obj(
        "registrationID" -> "12345",
        "language" -> "EN"
      )

      def stubRetrieveCurrentProfile(status: Int, body: Option[JsValue] = None) = {
        val response = body match {
          case Some(value) => aResponse().withStatus(status).withBody(value.toString)
          case None => aResponse().withStatus(status)
        }

        stubFor(get(urlPathEqualTo(s"/business-registration/business-tax-registration")).willReturn(response))
      }

      "handle a successful response returning the expected Business Profile" in {

        stubRetrieveCurrentProfile(OK, Some(businessProfileJson))

        val result = await(connector.retrieveCurrentProfile)

        result mustBe businessProfile
      }

      "handle any other response returning an BusinessRegistrationException" in {

        stubRetrieveCurrentProfile(INTERNAL_SERVER_ERROR, None)

        intercept[DownstreamExceptions.BusinessRegistrationException](await(connector.retrieveCurrentProfile))
      }
    }

    "calling .retrieveCompletionCapacity()" must {

      val completionCapacityJson = Json.obj(
        "completionCapacity" -> "foo"
      )

      def stubRetrieveCompletionCapacity(status: Int, body: Option[JsValue] = None) = {
        val response = body match {
          case Some(value) => aResponse().withStatus(status).withBody(value.toString)
          case None => aResponse().withStatus(status)
        }

        stubFor(get(urlPathEqualTo(s"/business-registration/business-tax-registration")).willReturn(response))
      }

      "handle a successful response returning the expected Completion Capacity" in {

        stubRetrieveCompletionCapacity(OK, Some(completionCapacityJson))

        val result = await(connector.retrieveCompletionCapacity)

        result mustBe Some("foo")
      }

      "handle a successful response returning NO Completion Capacity by returning 'None'" in {

        stubRetrieveCompletionCapacity(OK, Some(Json.obj()))

        val result = await(connector.retrieveCompletionCapacity)

        result mustBe None
      }

      "handle a NOT_FOUND returning 'None'" in {

        stubRetrieveCompletionCapacity(NOT_FOUND, None)

        val result = await(connector.retrieveCompletionCapacity)

        result mustBe None
      }

      "handle any other response returning an BusinessRegistrationException" in {

        stubRetrieveCompletionCapacity(INTERNAL_SERVER_ERROR, None)

        intercept[DownstreamExceptions.BusinessRegistrationException](await(connector.retrieveCompletionCapacity))
      }
    }

    "for the Trading Name" when {

      val tradingName = "trade"
      val tradingNameJson = Json.obj(
        "tradingName" -> tradingName
      )

      "calling .retrieveTradingName(regId: String)" must {

        def stubRetrieveTradingName(status: Int, body: Option[JsValue] = None) = {
          val response = body match {
            case Some(value) => aResponse().withStatus(status).withBody(value.toString)
            case None => aResponse().withStatus(status)
          }

          stubFor(get(urlPathEqualTo(s"/business-registration/$regId/trading-name")).willReturn(response))
        }

        "handle a successful response returning the expected Trading Name" in {

          stubRetrieveTradingName(OK, Some(tradingNameJson))

          val result = await(connector.retrieveTradingName(regId))

          result mustBe Some(tradingName)
        }

        "handle a successful response with no Trading Name returning 'None'" in {

          stubRetrieveTradingName(OK, Some(Json.obj()))

          val result = await(connector.retrieveTradingName(regId))

          result mustBe None
        }

        "handle a NOT_FOUND returning 'None'" in {

          stubRetrieveTradingName(NOT_FOUND, None)

          val result = await(connector.retrieveTradingName(regId))

          result mustBe None
        }

        "handle any other response returning a 'None'" in {

          stubRetrieveTradingName(INTERNAL_SERVER_ERROR, None)

          val result = await(connector.retrieveTradingName(regId))

          result mustBe None
        }
      }

      "calling .upsertTradingName(regId: String, tradingName: String)" must {

        def stubUpsertTradingName(newName: JsValue)(status: Int) = {
          stubFor(
            post(urlPathEqualTo(s"/business-registration/$regId/trading-name"))
              .withRequestBody(equalToJson(newName.toString))
              .willReturn(aResponse().withStatus(status))
          )
        }

        "handle a successful response by returning the new name that was passed in" in {

          stubUpsertTradingName(tradingNameJson)(OK)

          val result = await(connector.upsertTradingName(regId, tradingName))

          result mustBe tradingName
        }

        "handle an error response by returning the name that was passed in" in {

          stubUpsertTradingName(tradingNameJson)(INTERNAL_SERVER_ERROR)

          val result = await(connector.upsertTradingName(regId, tradingName))

          result mustBe tradingName
        }
      }
    }

    "for the Contact Details" when {

      val payeContactDetails = PAYEContactDetails(
        "First Middle Last",
        DigitalContactDetails(
          Some("test@example.com"),
          Some("01234567890"),
          Some("01234567890")
        )
      )
      val payeContactDetailsJson = Json.obj(
        "firstName" -> "First",
        "middleName" -> "Middle",
        "surname" -> "Last",
        "email" -> "test@example.com",
        "telephoneNumber" -> "01234567890",
        "mobileNumber" -> "01234567890"
      )

      "calling .retrieveContactDetails(regId: String)" must {

        def stubRetrieveContactDetails(status: Int, body: Option[JsValue] = None) = {
          val response = body match {
            case Some(value) => aResponse().withStatus(status).withBody(value.toString)
            case None => aResponse().withStatus(status)
          }

          stubFor(get(urlPathEqualTo(s"/business-registration/$regId/contact-details")).willReturn(response))
        }

        "handle a successful response returning the expected Contact Details" in {

          stubRetrieveContactDetails(OK, Some(payeContactDetailsJson))

          val result = await(connector.retrieveContactDetails(regId))

          result mustBe Some(payeContactDetails)
        }

        "handle a NOT_FOUND returning 'None'" in {

          stubRetrieveContactDetails(NOT_FOUND, None)

          val result = await(connector.retrieveContactDetails(regId))

          result mustBe None
        }

        "handle any other response returning a 'None'" in {

          stubRetrieveContactDetails(INTERNAL_SERVER_ERROR, None)

          val result = await(connector.retrieveContactDetails(regId))

          result mustBe None
        }
      }

      "calling .upsertContactDetails(regId: String)" must {

        def stubUpsertTradingName(contactDetails: JsValue)(status: Int) = {
          stubFor(
            post(urlPathEqualTo(s"/business-registration/$regId/contact-details"))
              .withRequestBody(equalToJson(contactDetails.toString))
              .willReturn(aResponse().withStatus(status))
          )
        }

        "handle a successful response returning the submitted Contact Details" in {

          stubUpsertTradingName(payeContactDetailsJson)(OK)

          val result = await(connector.upsertContactDetails(regId, payeContactDetails))

          result mustBe payeContactDetails
        }

        "handle any other response returning the submitted Contact Details" in {

          stubUpsertTradingName(payeContactDetailsJson)(INTERNAL_SERVER_ERROR)

          val result = await(connector.upsertContactDetails(regId, payeContactDetails))

          result mustBe payeContactDetails
        }
      }
    }

    "for the Addresses" when {

      val address = Address(
        line1 = "Test House",
        line2 = "The Tests",
        line3 = Some("Line 3"),
        line4 = Some("Line 4"),
        postCode = Some("BB00 0BB"),
        country = None,
        auditRef = Some("ref")
      )
      val addressJson = Json.obj(
        "addressLine1" -> "Test House",
        "addressLine2" -> "The Tests",
        "addressLine3" -> "Line 3",
        "addressLine4" -> "Line 4",
        "postcode" -> "BB00 0BB",
        "auditRef" -> "ref"
      )
      val addressesJson = Json.obj("addresses" -> Json.arr(addressJson))

      "calling .retrieveAddresses(regId: String)" must {

        def stubRetrieveAddresses(status: Int, body: Option[JsValue] = None) = {
          val response = body match {
            case Some(value) => aResponse().withStatus(status).withBody(value.toString)
            case None => aResponse().withStatus(status)
          }

          stubFor(get(urlPathEqualTo(s"/business-registration/$regId/addresses")).willReturn(response))
        }

        "handle a successful response returning the expected Addresses" in {

          stubRetrieveAddresses(OK, Some(addressesJson))

          val result = await(connector.retrieveAddresses(regId))

          result mustBe Seq(address)
        }

        "handle a NOT_FOUND returning 'None'" in {

          stubRetrieveAddresses(NOT_FOUND, None)

          val result = await(connector.retrieveAddresses(regId))

          result mustBe Seq()
        }

        "handle any other response returning a 'None'" in {

          stubRetrieveAddresses(INTERNAL_SERVER_ERROR, None)

          val result = await(connector.retrieveAddresses(regId))

          result mustBe Seq()
        }
      }

      "calling .upsertContactDetails(regId: String)" must {

        def stubUpsertAddress(address: JsValue)(status: Int) = {
          stubFor(
            post(urlPathEqualTo(s"/business-registration/$regId/addresses"))
              .withRequestBody(equalToJson(address.toString))
              .willReturn(aResponse().withStatus(status))
          )
        }

        "handle a successful response returning the submitted Address" in {

          stubUpsertAddress(addressJson)(OK)

          val result = await(connector.upsertAddress(regId, address))

          result mustBe address
        }

        "handle any other response returning the submitted Address" in {

          stubUpsertAddress(addressJson)(INTERNAL_SERVER_ERROR)

          val result = await(connector.upsertAddress(regId, address))

          result mustBe address
        }
      }
    }
  }
}
