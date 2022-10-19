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
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.Address
import models.external._
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

class AddressLookupConnectorISpec extends IntegrationSpecBase {

  implicit val hc = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map("microservice.services.address-lookup-frontend.port" -> s"${WiremockHelper.wiremockPort}"))
    .build

  lazy val connector = app.injector.instanceOf[AddressLookupConnector]

  "AddressLookupConnector" when {

    "calling .getAddress()" must {

      val addressId = "12345"

      val testAddress = Address(
        line1 = "Test House",
        line2 = "The Tests",
        line3 = None,
        line4 = None,
        postCode = Some("BB00 0BB"),
        country = None,
        auditRef = None
      )

      val testAddressJson = Json.obj(
        "id" -> "GB200000706253",
        "uprn" -> 706253,
        "parentUprn" -> 706251,
        "usrn" -> 706253,
        "organisation" -> "Some Company",
        "address" -> Json.obj(
          "lines" -> Json.arr(
            "Test House",
            "The Tests"
          ),
          "town" -> "Test Town",
          "postcode" -> "BB00 0BB",
          "subdivision" -> Json.obj(
            "code" -> "GB-ENG",
            "name" -> "England"
          ),
          "country" -> Json.obj(
            "code" -> "GB",
            "name" -> "United Kingdom"
          )
        ),
        "localCustodian" -> Json.obj(
          "code" -> 1760,
          "name" -> "Test Valley"
        ),
        "location" -> Json.arr(
          50.9986451,
          -1.4690977
        ),
        "language" -> "en",
        "administrativeArea" -> "TEST COUNTY"
      )

      def stubGetAddress(status: Int, body: Option[JsValue] = None) = {
        val response = body match {
          case Some(value) => aResponse().withStatus(status).withBody(value.toString)
          case None => aResponse().withStatus(status)
        }

        stubFor(get(urlPathEqualTo(s"/api/confirmed")).withQueryParam("id", equalTo(addressId)).willReturn(response))
      }

      "handle a successful response returning the expected Address" in {

        stubGetAddress(200, Some(testAddressJson))

        val result = await(connector.getAddress(addressId))

        result mustBe testAddress
      }

      "handle a NOT_FOUND returning the NOT_FOUND_EXCEPTION" in {

        stubGetAddress(404, None)

        intercept[NotFoundException](await(connector.getAddress(addressId)))
      }

      "handle any other response returning an UpstreamErrorResponse" in {

        stubGetAddress(500, None)

        intercept[UpstreamErrorResponse](await(connector.getAddress(addressId)))
      }
    }

    "calling .getOnRampUrl()" must {

      val alfJourneyConfig: AlfJourneyConfig = AlfJourneyConfig(
        version = AlfJourneyConfig.defaultConfigVersion,
        options = JourneyOptions(
          continueUrl = "http://localhost:9870/register-for-paye/return-from-address-for-ppob",
          homeNavHref = "http://www.hmrc.gov.uk/",
          accessibilityFooterUrl = "http://localhost:12346/accessibility-statement/paye-registration",
          deskProServiceName = "SCRS",
          showPhaseBanner = true,
          alphaPhase = false,
          showBackButtons = true,
          includeHMRCBranding = false,
          disableTranslations = false,


          selectPageConfig = SelectPageConfig(
            proposalListLimit = 30,
            showSearchAgainLink = true
          ),

          confirmPageConfig = ConfirmPageConfig(
            showSearchAgainLink = false,
            showSubHeadingAndInfo = false,
            showChangeLink = true
          ),

          timeoutConfig = TimeoutConfig(
            timeoutAmount = 900,
            timeoutUrl = "http://localhost:9870/register-for-paye/error/timeout"
          )
        ),
        labels = JourneyLabels(en = LanguageLabels(
          appLevelLabels = AppLevelLabels(
            navTitle = Some("Register an employer for PAYE"),
            phaseBannerHtml = Some("This is a new service. Help us improve it - send your <a href=\"https://www.tax.service.gov.uk/register-for-paye/feedback\">feedback</a>.")
          ),
          SelectPageLabels(
            title = Some("Choose an address"),
            heading = Some("Choose an address"),
            searchAgainLinkText = Some("Search again"),
            editAddressLinkText = Some("Edit address manually")
          ),
          LookupPageLabels(
            title = Some("Search for your address"),
            heading = Some("Search for your address"),
            filterLabel = Some("House name or number (optional)"),
            submitLabel = Some("Search address"),
            manualAddressLinkText = Some("The address doesn’t have a UK postcode")
          ),
          EditPageLabels(
            title = Some("Enter address"),
            heading = Some("Enter address"),
            line1Label = Some("Address line 1"),
            line2Label = Some("Address line 2"),
            line3Label = Some("Address line 3")
          ),
          ConfirmPageLabels(
            title = Some("Review and confirm your address"),
            heading = Some("Review and confirm your address"),
            submitLabel = Some("Save and continue"),
            changeLinkText = Some("Change")
          )
        ),
          cy = LanguageLabels(
            appLevelLabels = AppLevelLabels(
              navTitle = Some("Cofrestru cyflogwr ar gyfer TWE"),
              phaseBannerHtml = Some("""Mae hwn yn wasanaeth newydd. Helpwch ni i’w wella – anfonwch eich <a href="https://www.tax.service.gov.uk/register-for-paye/feedback">adborth</a>.""")
            ),
            SelectPageLabels(
              title = Some("Dewiswch gyfeiriad"),
              heading = Some("Dewiswch gyfeiriad"),
              searchAgainLinkText = Some("Chwilio eto"),
              editAddressLinkText = Some("Golygwch y cyfeiriad â llaw")
            ),
            LookupPageLabels(
              title = Some("Chwiliwch am eich cyfeiriad"),
              heading = Some("Chwiliwch am eich cyfeiriad"),
              filterLabel = Some("Enw neu rif y tŷ (dewisol)"),
              submitLabel = Some("Chwilio am y cyfeiriad"),
              manualAddressLinkText = Some("Nid oes gan y cyfeiriad god post yn y DU")
            ),
            EditPageLabels(
              title = Some("Nodwch gyfeiriad"),
              heading = Some("Nodwch gyfeiriad"),
              line1Label = Some("Cyfeiriad - llinell 1"),
              line2Label = Some("Cyfeiriad - llinell 2"),
              line3Label = Some("Cyfeiriad - llinell 3")
            ),
            ConfirmPageLabels(
              title = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              heading = Some("Adolygu a chadarnhau’ch cyfeiriad"),
              submitLabel = Some("Cadw ac yn eich blaen"),
              changeLinkText = Some("Newid")
            )
          )
        )
      )

      def stubAddressOnRamp(requestBody: JsValue)(status: Int, responseHeaders: Map[String, String] = Map()) = {

        val headers = new HttpHeaders().plus(responseHeaders.map(kv => new HttpHeader(kv._1, kv._2)).toSeq:_*)

        stubFor(
          post(urlPathEqualTo(s"/api/v2/init"))
            .withRequestBody(equalToJson(requestBody.toString))
            .willReturn(aResponse().withHeaders(headers))
        )
      }

      "handle a successful response returning the expected Address" in {

        stubAddressOnRamp(Json.toJson(alfJourneyConfig))(NO_CONTENT, Map(HeaderNames.LOCATION -> "/foo/bar/wizz"))

        val result = await(connector.getOnRampUrl(alfJourneyConfig))

        result mustBe "/foo/bar/wizz"
      }

      "handle a NOT_FOUND returning ALFLocationHeaderNotSetException" in {

        stubAddressOnRamp(Json.toJson(alfJourneyConfig))(NOT_FOUND)

        intercept[ALFLocationHeaderNotSetException](await(connector.getOnRampUrl(alfJourneyConfig)))
      }

      "handle any other response returning ALFLocationHeaderNotSetException" in {

        stubAddressOnRamp(Json.toJson(alfJourneyConfig))(INTERNAL_SERVER_ERROR)

        intercept[ALFLocationHeaderNotSetException](await(connector.getOnRampUrl(alfJourneyConfig)))
      }
    }
  }
}
