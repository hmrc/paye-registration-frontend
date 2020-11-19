/*
 * Copyright 2020 HM Revenue & Customs
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
import common.exceptions.DownstreamExceptions.OfficerListNotFoundException
import config.{AppConfig, WSHttpImpl}
import connectors.{IncorpInfoSuccessResponse, IncorporationInformationConnectorImpl}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.Address
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Mode}
import services.MetricsService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.PAYEFeatureSwitch

class IncorporationInformationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val incorpInfoUri = "/incorpInfoUri"
  val stubbedUri = "/stubbedUri"

  val config = Map(
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJsaW5lMSI6IjMwIFRlc3QgUm9hZCIsImxpbmUyIjoiVGVzdGxleSIsImxpbmUzIjoiVGVzdGZvcmQiLCJsaW5lNCI6IlRlc3RzaGlyZSIsInBvc3RDb2RlIjoiVEUxIDNTVCJ9",
    "defaultOfficerList" -> "ICBbDQogICAgew0KICAgICAgIm5hbWUiIDogInRlc3QiLA0KICAgICAgIm5hbWVfZWxlbWVudHMiIDogew0KICAgICAgICAiZm9yZW5hbWUiIDogInRlc3QxIiwNCiAgICAgICAgIm90aGVyX2ZvcmVuYW1lcyIgOiAidGVzdDExIiwNCiAgICAgICAgInN1cm5hbWUiIDogInRlc3RhIiwNCiAgICAgICAgInRpdGxlIiA6ICJNciINCiAgICAgIH0sDQogICAgICAib2ZmaWNlcl9yb2xlIiA6ICJjaWMtbWFuYWdlciINCiAgICB9LCB7DQogICAgICAibmFtZSIgOiAidGVzdCIsDQogICAgICAibmFtZV9lbGVtZW50cyIgOiB7DQogICAgICAgICJmb3JlbmFtZSIgOiAidGVzdDIiLA0KICAgICAgICAib3RoZXJfZm9yZW5hbWVzIiA6ICJ0ZXN0MjIiLA0KICAgICAgICAic3VybmFtZSIgOiAidGVzdGIiLA0KICAgICAgICAidGl0bGUiIDogIk1yIg0KICAgICAgfSwNCiAgICAgICJvZmZpY2VyX3JvbGUiIDogImNvcnBvcmF0ZS1kaXJlY3RvciINCiAgICB9DQogIF0=",
    "microservice.services.incorporation-information.uri" -> incorpInfoUri
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build


  class Setup {
    lazy val metrics = app.injector.instanceOf[MetricsService]
    lazy val featureSwitch = app.injector.instanceOf[PAYEFeatureSwitch]
    lazy val http = app.injector.instanceOf[WSHttpImpl]
    implicit lazy val appConfig = app.injector.instanceOf[AppConfig]

    val incorpInfoConnector = new IncorporationInformationConnectorImpl(
      metrics,
      http
    )
  }

  val testTransId = "testTransId"
  val testRegId = "testRegId"
  implicit val hc = HeaderCarrier()

  "getCoHoCompanyDetails" should {

    val url = s"$incorpInfoUri/$testTransId/company-profile"

    val deetsModel =
      CoHoCompanyDetailsModel(
        "test company",
        Address(
          "1 test street, (P-O I&O)",
          "Testford",
          None,
          None,
          Some("TE2 2ST")
        )
      )

    def setupWiremockResult(status: Int, body: String) =
      stubFor(get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
      )

    val testCompanyDetailsJson =
      """
        |{
        |  "company_name":"test company",
        |  "registered_office_address":{
        |    "premises":"1",
        |    "address_line_1":"test street, (P-O: I&O)",
        |    "locality":"Testford",
        |    "country":"UK",
        |    "postal_code":"TE2 2ST"
        |  }
        |}
        """.stripMargin

    "fetch and convert company details" in new Setup {


      def getResponse = incorpInfoConnector.getCoHoCompanyDetails("regID", testTransId)

      setupWiremockResult(200, testCompanyDetailsJson)

      await(getResponse) mustBe IncorpInfoSuccessResponse(deetsModel)
    }

    "get a default CoHo Company Details when the regId is part of the allow-list" in new Setup {
      val regIdAllowlisted = "regAllowlist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"
      val defaultROAddress = Address(
        "30 Test Road",
        "Testley",
        Some("Testford"),
        Some("Testshire"),
        Some("TE1 3ST"),
        None
      )
      val incorpInfoSuccessCoHoCompanyDetails = IncorpInfoSuccessResponse(
        CoHoCompanyDetailsModel(
          defaultCompanyName,
          defaultROAddress)
      )


      def getResponse = incorpInfoConnector.getCoHoCompanyDetails(regIdAllowlisted, "txID")

      await(getResponse) mustBe incorpInfoSuccessCoHoCompanyDetails
    }
  }

  "getOfficerList" should {
    val url = s"$incorpInfoUri/$testTransId/officer-list"

    val tstOfficerListModel = OfficerList(
      items = Seq(
        Officer(
          name = Name(Some("test1"), Some("test11"), "testa", Some("Mr")),
          role = "cic-manager",
          resignedOn = None,
          appointmentLink = None
        ),
        Officer(
          name = Name(Some("test2"), Some("test22"), "testb", Some("Mr")),
          role = "corporate-director",
          resignedOn = None,
          appointmentLink = None
        )
      )
    )

    def setupWiremockResult(status: Int, body: String) =
      stubFor(get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
      )

    val tstOfficerListJson =
      """
        |{
        |  "officers": [
        |    {
        |      "name" : "test",
        |      "name_elements" : {
        |        "forename" : "test1",
        |        "other_forenames" : "test11",
        |        "surname" : "testa",
        |        "title" : "Mr"
        |      },
        |      "officer_role" : "cic-manager"
        |    }, {
        |      "name" : "test",
        |      "name_elements" : {
        |        "forename" : "test2",
        |        "other_forenames" : "test22",
        |        "surname" : "testb",
        |        "title" : "Mr"
        |      },
        |      "officer_role" : "corporate-director"
        |    }
        |  ]
        |}""".stripMargin

    "get an officer list" in new Setup {


      def getResponse = incorpInfoConnector.getOfficerList(testTransId, testRegId)

      setupWiremockResult(200, tstOfficerListJson)

      await(getResponse) mustBe tstOfficerListModel
    }

    "get an officer list and normalise the first, middle, last names and title of the directors" in new Setup {

      val tstOfficerListJson =
        """
          |{
          |  "officers": [
          |    {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "tęšt1",
          |        "other_forenames" : "tèēśt11",
          |        "surname" : "tæßt.",
          |        "title" : "Mr,"
          |      },
          |      "officer_role" : "cic-manager"
          |    }, {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "žñœü",
          |        "other_forenames" : "ÿürgëñ",
          |        "surname" : "alfîë",
          |        "title" : "Mr"
          |      },
          |      "officer_role" : "corporate-director"
          |    }
          |  ]
          |}""".stripMargin

      val tstOfficerListModel = OfficerList(
        items = Seq(
          Officer(
            name = Name(Some("test1"), Some("teest11"), "tt", Some("Mr")),
            role = "cic-manager",
            resignedOn = None,
            appointmentLink = None
          ),
          Officer(
            name = Name(Some("znu"), Some("yurgen"), "alfie", Some("Mr")),
            role = "corporate-director",
            resignedOn = None,
            appointmentLink = None
          )
        )
      )

      def getResponse = incorpInfoConnector.getOfficerList(testTransId, testRegId)

      setupWiremockResult(200, tstOfficerListJson)

      await(getResponse) mustBe tstOfficerListModel
    }

    "get an officer list from config if the regId is allow-list and every other service returns none" in new Setup {

      def getResponse = incorpInfoConnector.getOfficerList(testTransId, "regAllowlist456")

      setupWiremockResult(404, "")
      await(getResponse) mustBe tstOfficerListModel
    }

    "throw an OfficerListNotFoundException when CoHo API returns an empty list" in new Setup {
      setupWiremockResult(200, """{"officers": []}""")

      intercept[OfficerListNotFoundException](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "throw an OfficerListNotFoundException for a 404 response" in new Setup {
      setupWiremockResult(404, "")

      intercept[OfficerListNotFoundException](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "throw a BadRequestException" in new Setup {
      setupWiremockResult(400, "")

      intercept[BadRequestException](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "throw an Exception" in new Setup {

      setupWiremockResult(500, "")

      intercept[Exception](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "get an officer list from II when stub is in place for other II calls" in new Setup {

      def getResponse = incorpInfoConnector.getOfficerList(testTransId, testRegId)

      setupWiremockResult(200, tstOfficerListJson)

      await(getResponse) mustBe tstOfficerListModel
    }
  }
}