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
import connectors.{IncorpInfoSuccessResponse, IncorporationInformationConnector}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.api.Name
import models.external.{CHROAddress, CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import services.MetricsService
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import utils.PAYEFeatureSwitch

class IncorporationInformationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val metrics = Play.current.injector.instanceOf[MetricsService]
  lazy val featureSwitch = Play.current.injector.instanceOf[PAYEFeatureSwitch]

  val incorpInfoUri = "/incorpInfoUri"
  val stubbedUri = "/stubbedUri"

  val additionalConfiguration = Map(
    "microservice.services.incorporation-frontend-stubs.host" -> s"$mockHost",
    "microservice.services.incorporation-frontend-stubs.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "microservice.services.incorporation-information.uri" -> incorpInfoUri,
    "microservice.services.incorporation-frontend-stubs.uri" -> stubbedUri
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val testTransId = "testTransId"
  implicit val hc = HeaderCarrier()

  def stub() = await(buildClient("/test-only/feature-flag/incorporationInformation/false").get())
  def unStub() = await(buildClient("/test-only/feature-flag/incorporationInformation/true").get())

  "getCoHoCompanyDetails" should {
    "get a default CoHo Company Details when the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"
      val incorpInfoSuccessCoHoCompanyDetails = IncorpInfoSuccessResponse(
                                                  CoHoCompanyDetailsModel(
                                                    regIdWhitelisted,
                                                    defaultCompanyName,
                                                    Seq.empty)
                                                )

      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)
      def getResponse = incorpInfoConnector.getCoHoCompanyDetails(regIdWhitelisted)

      await(getResponse) shouldBe incorpInfoSuccessCoHoCompanyDetails
    }
  }

  "getRegisteredOfficeAddress" should {
    val url = s"$incorpInfoUri/$testTransId/ro-address"

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

    def setupWiremockResult(status: Int, body: String) =
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

      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)
      unStub()
      def getResponse = incorpInfoConnector.getRegisteredOfficeAddress(testTransId)

      setupWiremockResult(200, testAddress)

      await(getResponse) shouldBe addressModel
    }

    "throw a BadRequestException" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getRegisteredOfficeAddress(testTransId)
      unStub()
      setupWiremockResult(400, "")

      intercept[BadRequestException](await(incorpInfoConnector.getRegisteredOfficeAddress(testTransId)))
    }

    "throw an Exception" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getRegisteredOfficeAddress(testTransId)
      unStub()
      setupWiremockResult(500, "")

      intercept[Exception](await(incorpInfoConnector.getRegisteredOfficeAddress(testTransId)))
    }

    "get a registered office address when stubbed" in {
      val stubbedUrl = s"$stubbedUri/$testTransId/ro-address"

      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)
      stub()
      def getResponse = incorpInfoConnector.getRegisteredOfficeAddress(testTransId)

      stubFor(get(urlMatching(stubbedUrl))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testAddress)
        )
      )

      await(getResponse) shouldBe addressModel
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
      """[
        |  {
        |    "name" : "test",
        |    "name_elements" : {
        |      "forename" : "test1",
        |      "other_forenames" : "test11",
        |      "surname" : "testa",
        |      "title" : "Mr"
        |    },
        |    "officer_role" : "cic-manager"
        |  }, {
        |    "name" : "test",
        |    "name_elements" : {
        |      "forename" : "test2",
        |      "other_forenames" : "test22",
        |      "surname" : "testb",
        |      "title" : "Mr"
        |    },
        |    "officer_role" : "corporate-director"
        |  }
        |]""".stripMargin

    "get an officer list" in {

      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getOfficerList(testTransId)
      unStub()
      setupWiremockResult(200, tstOfficerListJson)

      await(getResponse) shouldBe tstOfficerListModel
    }

    "get an empty officer list when CoHo API returns a NotFoundException" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getOfficerList(testTransId)
      unStub()
      setupWiremockResult(404, "")

      await(getResponse) shouldBe OfficerList(items = Nil)
    }

    "throw a BadRequestException" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getOfficerList(testTransId)
      unStub()
      setupWiremockResult(400, "")

      intercept[BadRequestException](await(incorpInfoConnector.getOfficerList(testTransId)))
    }

    "throw an Exception" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getOfficerList(testTransId)
      unStub()
      setupWiremockResult(500, "")

      intercept[Exception](await(incorpInfoConnector.getOfficerList(testTransId)))
    }

    "get an officer list from II when stub is in place for other II calls" in {
      val incorpInfoConnector = new IncorporationInformationConnector(featureSwitch, metrics)

      def getResponse = incorpInfoConnector.getOfficerList(testTransId)
      stub()
      setupWiremockResult(200, tstOfficerListJson)

      await(getResponse) shouldBe tstOfficerListModel
    }
  }
}