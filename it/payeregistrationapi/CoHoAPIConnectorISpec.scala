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
package payeregistrationapi

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.CoHoAPIConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external.CHROAddress
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}

class CoHoAPIConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.coho-api.host" -> s"$mockHost",
    "microservice.services.coho-api.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val testTransId = "testTransId"
  implicit val hc = HeaderCarrier()

  val url = s"/incorporation-frontend-stubs/$testTransId/ro-address"

  "getRegisteredOfficeAddress" should {

    val addressModel =
      CHROAddress(
        "12",
        "test road",
        None,
        "test town",
        None,
        None,
        None,
        None
      )

    def setupStubResult(status: Int, body: String) =
      stubFor(get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
      )

    val testAddress =
        """
          |{
          |   "premises" : "12",
          |   "address_line_1" : "test road",
          |   "locality" : "test town"
          |}
        """.stripMargin

    "get a registered office address" in {

      val cohoAPIConnector = new CoHoAPIConnector()

      def getResponse = cohoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(200, testAddress)

      await(getResponse) shouldBe addressModel
    }

    "throw a BadRequestException" in {
      val coHoAPIConnector = new CoHoAPIConnector()

      def getResponse = coHoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(400, "")

      intercept[BadRequestException](await(coHoAPIConnector.getRegisteredOfficeAddress(testTransId)))
    }

    "throw an Exception" in {
      val coHoAPIConnector = new CoHoAPIConnector()

      def getResponse = coHoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(500, "")

      intercept[Exception](await(coHoAPIConnector.getRegisteredOfficeAddress(testTransId)))
    }
  }
}