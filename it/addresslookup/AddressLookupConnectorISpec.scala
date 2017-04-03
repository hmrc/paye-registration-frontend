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
package addresslookup

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{ALFLocationHeaderNotSetException, AddressLookupConnector}
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Call
import uk.gov.hmrc.play.http.HeaderCarrier

class AddressLookupConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val mockPAYEFrontendUrl = "/paye-frontend-test-url"

  val additionalConfiguration = Map(
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "microservice.services.paye-registration-frontend.www.url" -> s"$mockPAYEFrontendUrl",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build()

  val testId = "12345"

  implicit val hc = HeaderCarrier()

  "getAddress" should {
    val testAddress = Json.obj("test" -> "address")

    "get an address from a 200" in {
      val addressLookupConnector = new AddressLookupConnector()

      def getAddress = addressLookupConnector.getAddress(testId)

      stubFor(get(urlMatching(s"/api/confirmed\\?id\\=$testId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testAddress.toString())
        )
      )

      await(getAddress) shouldBe testAddress
    }
  }

  "getOnRampUrl" should {

    "get an address lookup start url" in {
      val query = "tst-query"
      lazy val call: Call = controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress()
      val tstALFUrl = """/test-alf/start-url"""

      val addressLookupConnector = new AddressLookupConnector()

      def getOnRamp = addressLookupConnector.getOnRampUrl(query, call)

      stubFor(post(urlMatching(s"/api/init/$query"))
        .withRequestBody(matchingJsonPath(s"[?(@.continueUrl == '$mockPAYEFrontendUrl${call.url}')]"))
          .willReturn(
            aResponse()
              .withStatus(202)
              .withHeader("Location", tstALFUrl)
          )
      )

      await(getOnRamp) shouldBe tstALFUrl
    }

    "throw the correct exception when no url is returned from ALF" in {
      val query = "tst-query"
      lazy val call: Call = controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress()
      val tstALFUrl = """/test-alf/start-url"""

      val addressLookupConnector = new AddressLookupConnector()

      def getOnRamp = addressLookupConnector.getOnRampUrl(query, call)

      stubFor(post(urlMatching(s"/api/init/$query"))
        .withRequestBody(matchingJsonPath(s"[?(@.continueUrl == '$mockPAYEFrontendUrl${call.url}')]"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      intercept[ALFLocationHeaderNotSetException]( await(getOnRamp) )
    }
}

}