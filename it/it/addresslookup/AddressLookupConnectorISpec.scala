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
package it.addresslookup

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.AddressLookupConnector
import it.itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier

class AddressLookupConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val testId = "12345"

  implicit val hc = HeaderCarrier()

  "getAddress" should {
    val testAddress = Json.obj("test" -> "address")

    "get an address from a 200" in {
      val addressLookupConnector = new AddressLookupConnector()

      def getAddress = addressLookupConnector.getAddress(testId)

      stubFor(get(urlMatching(s"/lookup-address/outcome/payereg1/$testId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testAddress.toString)
        )
      )

      await(getAddress) shouldBe testAddress
    }
  }

}