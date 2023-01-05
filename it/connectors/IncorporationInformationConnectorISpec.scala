/*
 * Copyright 2023 HM Revenue & Customs
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
import common.exceptions.DownstreamExceptions.OfficerListNotFoundException
import config.AppConfig
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.Address
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.MetricsService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient}
import utils.PAYEFeatureSwitch

import scala.concurrent.ExecutionContext

class IncorporationInformationConnectorISpec extends IntegrationSpecBase {

  implicit val request: FakeRequest[_] = FakeRequest()

  val incorpInfoUri = "/incorpInfoUri"

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map(
      "microservice.services.incorporation-information.port" -> s"${WiremockHelper.wiremockPort}",
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "microservice.services.incorporation-information.uri" -> incorpInfoUri,
      "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
      "defaultCTStatus" -> "aGVsZA==",
      "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
      "defaultCHROAddress" -> "eyJsaW5lMSI6IjMwIFRlc3QgUm9hZCIsImxpbmUyIjoiVGVzdGxleSIsImxpbmUzIjoiVGVzdGZvcmQiLCJsaW5lNCI6IlRlc3RzaGlyZSIsInBvc3RDb2RlIjoiVEUxIDNTVCJ9",
      "defaultOfficerList" -> "ICBbDQogICAgew0KICAgICAgIm5hbWUiIDogInRlc3QiLA0KICAgICAgIm5hbWVfZWxlbWVudHMiIDogew0KICAgICAgICAiZm9yZW5hbWUiIDogInRlc3QxIiwNCiAgICAgICAgIm90aGVyX2ZvcmVuYW1lcyIgOiAidGVzdDExIiwNCiAgICAgICAgInN1cm5hbWUiIDogInRlc3RhIiwNCiAgICAgICAgInRpdGxlIiA6ICJNciINCiAgICAgIH0sDQogICAgICAib2ZmaWNlcl9yb2xlIiA6ICJjaWMtbWFuYWdlciINCiAgICB9LCB7DQogICAgICAibmFtZSIgOiAidGVzdCIsDQogICAgICAibmFtZV9lbGVtZW50cyIgOiB7DQogICAgICAgICJmb3JlbmFtZSIgOiAidGVzdDIiLA0KICAgICAgICAib3RoZXJfZm9yZW5hbWVzIiA6ICJ0ZXN0MjIiLA0KICAgICAgICAic3VybmFtZSIgOiAidGVzdGIiLA0KICAgICAgICAidGl0bGUiIDogIk1yIg0KICAgICAgfSwNCiAgICAgICJvZmZpY2VyX3JvbGUiIDogImNvcnBvcmF0ZS1kaXJlY3RvciINCiAgICB9DQogIF0=",
    ))
    .build

  val incorpInfoConnector = app.injector.instanceOf[IncorporationInformationConnector]

  val testTransId = "testTransId"
  val testRegId = "testRegId"
  implicit val hc = HeaderCarrier()

  "getCoHoCompanyDetails" should {

    val companyDetails =
      CoHoCompanyDetailsModel(
        companyName = "test company",
        roAddress = Address(
          line1 = "1 test street, (P-O I&O)",
          line2 = "Testford",
          line3 = None,
          line4 = None,
          postCode = Some("TE2 2ST")
        )
      )

    val companyDetailsJson =
      Json.obj(
        "company_name" -> "test company",
        "registered_office_address" -> Json.obj(
          "premises" -> "1",
          "address_line_1" -> "test street, (P-O: I&O)",
          "locality" -> "Testford",
          "country" -> "UK",
          "postal_code" -> "TE2 2ST"
        )
      )

    def setupWiremockResult(status: Int, body: Option[JsValue]) =
      stubFor(get(urlMatching(s"$incorpInfoUri/$testTransId/company-profile")).willReturn(buildResponse(status, body)))

    "fetch and convert company details" in {

      setupWiremockResult(OK, Some(companyDetailsJson))

      await(incorpInfoConnector.getCoHoCompanyDetails("regID", testTransId)) mustBe IncorpInfoSuccessResponse(companyDetails)
    }

    "get a default CoHo Company Details when the regId is part of the allow-list" in {
      val regIdAllowlisted = "regAllowlist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"
      val defaultROAddress = Address(
        line1 = "30 Test Road",
        line2 = "Testley",
        line3 = Some("Testford"),
        line4 = Some("Testshire"),
        postCode = Some("TE1 3ST"),
        country = None
      )
      val incorpInfoSuccessCoHoCompanyDetails = IncorpInfoSuccessResponse(
        response = CoHoCompanyDetailsModel(
          companyName = defaultCompanyName,
          roAddress = defaultROAddress
        )
      )

      await(incorpInfoConnector.getCoHoCompanyDetails(regIdAllowlisted, "txID")) mustBe incorpInfoSuccessCoHoCompanyDetails
    }
  }

  "getOfficerList" should {

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

    val tstOfficerListJson =
      Json.obj(
        "officers" -> Json.arr(
          Json.obj(
            "name" -> "test",
            "name_elements" -> Json.obj(
              "forename" -> "test1",
              "other_forenames" -> "test11",
              "surname" -> "testa",
              "title" -> "Mr"
            ),
            "officer_role" -> "cic-manager"
          ),
          Json.obj(
            "name" -> "test",
            "name_elements" -> Json.obj(
              "forename" -> "test2",
              "other_forenames" -> "test22",
              "surname" -> "testb",
              "title" -> "Mr"
            ),
            "officer_role" -> "corporate-director"
          )
        )
      )

    def setupWiremockResult(status: Int, body: Option[JsValue]) =
      stubFor(get(urlMatching(s"$incorpInfoUri/$testTransId/officer-list")).willReturn(buildResponse(status, body)))

    "get an officer list" in {

      setupWiremockResult(OK, Some(tstOfficerListJson))

      await(incorpInfoConnector.getOfficerList(testTransId, testRegId)) mustBe tstOfficerListModel
    }

    "get an officer list and normalise the first, middle, last names and title of the directors" in {

      val tstOfficerListJson =
        Json.obj(
          "officers" -> Json.arr(
            Json.obj(
              "name" -> "test",
              "name_elements" -> Json.obj(
                "forename" -> "tęšt1",
                "other_forenames" -> "tèēśt11",
                "surname" -> "tæßt",
                "title" -> "Mr"
              ),
              "officer_role" -> "cic-manager"
            ),
            Json.obj(
              "name" -> "test",
              "name_elements" -> Json.obj(
                "forename" -> "žñœü",
                "other_forenames" -> "ÿürgëñ",
                "surname" -> "alfîë",
                "title" -> "Mr"
              ),
              "officer_role" -> "corporate-director"
            )
          )
        )

      val tstOfficerListModel = OfficerList(
        items = Seq(
          Officer(
            name = Name(Some("test1"), Some("teest11"), Some("tt"), Some("Mr")),
            role = "cic-manager",
            resignedOn = None,
            appointmentLink = None
          ),
          Officer(
            name = Name(Some("znu"), Some("yurgen"), Some("alfie"), Some("Mr")),
            role = "corporate-director",
            resignedOn = None,
            appointmentLink = None
          )
        )
      )

      setupWiremockResult(OK, Some(tstOfficerListJson))

      await(incorpInfoConnector.getOfficerList(testTransId, testRegId)) mustBe tstOfficerListModel
    }

    "get an officer list from config if the regId is allow-list and every other service returns none" in {

      setupWiremockResult(404, None)

      await(incorpInfoConnector.getOfficerList(testTransId, "regAllowlist456")) mustBe tstOfficerListModel
    }

    "throw an OfficerListNotFoundException when CoHo API returns an empty list" in {

      setupWiremockResult(OK, Some(Json.obj("officers" -> Json.arr())))

      intercept[OfficerListNotFoundException](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "throw an OfficerListNotFoundException for a 404 response" in {

      setupWiremockResult(NOT_FOUND, None)

      intercept[OfficerListNotFoundException](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "throw an Exception" in {

      setupWiremockResult(INTERNAL_SERVER_ERROR, None)

      intercept[Exception](await(incorpInfoConnector.getOfficerList(testTransId, testRegId)))
    }

    "get an officer list from II when stub is in place for other II calls" in {

      setupWiremockResult(OK, Some(tstOfficerListJson))

      await(incorpInfoConnector.getOfficerList(testTransId, testRegId)) mustBe tstOfficerListModel
    }
  }
}