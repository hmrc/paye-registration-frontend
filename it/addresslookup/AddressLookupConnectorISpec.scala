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
package addresslookup

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{ALFLocationHeaderNotSetException, AddressLookupConnector}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.Address
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

class AddressLookupConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val mockPAYEFrontendUrl = "/paye-frontend-test-url"

  val additionalConfiguration = Map(
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "microservice.services.paye-registration-frontend.www.url" -> s"$mockPAYEFrontendUrl",
    "timeoutInSeconds" -> 100,
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build()

  val testId = "12345"

  implicit val hc = HeaderCarrier()

  "getAddress" should {
    val testAddress = Json.parse(
      """{
        |  "address":{
        |    "lines":[
        |      "14 St Test Walker",
        |      "Testford"
        |    ],
        |    "postcode":"TE1 1ST"
        |  }
        |}""".stripMargin)

    val testAddressModel = Address(
      line1 = "14 St Test Walker",
      line2 = "Testford",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST"),
      country = None
    )

    "get an address from a 200" in {
      val addressLookupConnector = app.injector.instanceOf(classOf[AddressLookupConnector])

      def getAddress = addressLookupConnector.getAddress(testId)

      stubFor(get(urlMatching(s"/api/confirmed\\?id\\=$testId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testAddress.toString())
        )
      )

      await(getAddress) mustBe testAddressModel
    }

    "get an address from a 200 and trim the lines if they are too long with normaliseation" in {
      val testAddressLongLines = Json.parse(
        """{
          |  "address":{
          |    "lines":[
          |      "14 St Test Ælker on sæpid long road",
          |      "Tßtford"
          |    ],
          |    "postcode":"Œ1 1ST"
          |  }
          |}""".stripMargin)

      val testAddressModelTrimmed = Address(
        line1 = "14 St Test AElker on saepid",
        line2 = "Tsstford",
        line3 = None,
        line4 = None,
        postCode = Some("OE1 1ST"),
        country = None
      )

      val addressLookupConnector = app.injector.instanceOf(classOf[AddressLookupConnector])

      def getAddress = addressLookupConnector.getAddress(testId)

      stubFor(get(urlMatching(s"/api/confirmed\\?id\\=$testId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testAddressLongLines.toString())
        )
      )

      await(getAddress) mustBe testAddressModelTrimmed
    }
  }

  "getOnRampUrl" should {

    "get an address lookup start url" in {
      lazy val call: Call = controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress()
      val tstALFUrl = """/test-alf/start-url"""

      val addressLookupConnector = app.injector.instanceOf(classOf[AddressLookupConnector])

      def getOnRamp = addressLookupConnector.getOnRampUrl("test", call)

      stubFor(post(urlMatching(s"/api/init"))
          .willReturn(
            aResponse()
              .withStatus(202)
              .withHeader("Location", tstALFUrl)
          )
      )

      await(getOnRamp) mustBe tstALFUrl
    }

    "throw the correct exception when no url is returned from ALF" in {
      lazy val call: Call = controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress()
      val tstALFUrl = """/test-alf/start-url"""

      val addressLookupConnector = app.injector.instanceOf(classOf[AddressLookupConnector])

      def getOnRamp = addressLookupConnector.getOnRampUrl("test", call)

      stubFor(post(urlMatching(s"/api/init"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      intercept[ALFLocationHeaderNotSetException]( await(getOnRamp) )
    }
}

}