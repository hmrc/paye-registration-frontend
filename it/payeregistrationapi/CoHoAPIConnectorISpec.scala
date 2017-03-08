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
import models.api.Name
import models.external.{CHROAddress, Officer, OfficerList}
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import services.MetricsService
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}

class CoHoAPIConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val metrics = Play.current.injector.instanceOf[MetricsService]

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

  "getRegisteredOfficeAddress" should {
    val url = s"/incorporation-frontend-stubs/$testTransId/ro-address"

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

      val cohoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = cohoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(200, testAddress)

      await(getResponse) shouldBe addressModel
    }

    "throw a BadRequestException" in {
      val coHoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = coHoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(400, "")

      intercept[BadRequestException](await(coHoAPIConnector.getRegisteredOfficeAddress(testTransId)))
    }

    "throw an Exception" in {
      val coHoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = coHoAPIConnector.getRegisteredOfficeAddress(testTransId)

      setupStubResult(500, "")

      intercept[Exception](await(coHoAPIConnector.getRegisteredOfficeAddress(testTransId)))
    }
  }

  "getOfficerList" should {
    val url = s"/incorporation-frontend-stubs/$testTransId/officer-list"

    val tstOfficerListModel = OfficerList(
      items = Seq(
        Officer(
          name = Name(Some("test1"), Some("test11"), Some("testa"), Some("Mr")),
          role = "cic-manager",
          resignedOn = None,
          appointmentLink = None
        ),
        Officer(
          name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
          role = "corporate-director",
          resignedOn = None,
          appointmentLink = None
        )
      )
    )

    def setupStubResult(status: Int, body: String) =
      stubFor(get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
      )

    val tstOfficerListJson =
      """{
        |  "items" : [ {
        |    "name" : "test",
        |    "name_elements" : {
        |      "forename" : "test1",
        |      "honours" : "test",
        |      "other_forenames" : "test11",
        |      "surname" : "testa",
        |      "title" : "Mr"
        |    },
        |    "officer_role" : "cic-manager"
        |  }, {
        |    "name" : "test",
        |    "name_elements" : {
        |      "forename" : "test2",
        |      "honours" : "test",
        |      "other_forenames" : "test22",
        |      "surname" : "testb",
        |      "title" : "Mr"
        |    },
        |    "officer_role" : "corporate-director"
        |  } ]
        |}""".stripMargin

    "get an officer list" in {

      val cohoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = cohoAPIConnector.getOfficerList(testTransId)

      setupStubResult(200, tstOfficerListJson)

      await(getResponse) shouldBe tstOfficerListModel
    }

    "get an empty officer list when CoHo API returns a NotFoundException" in {
      val coHoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = coHoAPIConnector.getOfficerList(testTransId)

      setupStubResult(404, "")

      await(getResponse) shouldBe OfficerList(items = Nil)
    }

    "throw a BadRequestException" in {
      val coHoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = coHoAPIConnector.getOfficerList(testTransId)

      setupStubResult(400, "")

      intercept[BadRequestException](await(coHoAPIConnector.getOfficerList(testTransId)))
    }

    "throw an Exception" in {
      val coHoAPIConnector = new CoHoAPIConnector(metrics)

      def getResponse = coHoAPIConnector.getOfficerList(testTransId)

      setupStubResult(500, "")

      intercept[Exception](await(coHoAPIConnector.getOfficerList(testTransId)))
    }
  }
}