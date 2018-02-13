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

package connectors

import common.exceptions.DownstreamExceptions.OfficerListNotFoundException
import config.WSHttp
import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}

import scala.concurrent.Future

class IncorporationInformationConnectorSpec extends PayeComponentSpec with GuiceOneAppPerSuite {

  val testUrl = "testIIUrl"
  val testUri = "testIIUri"
  val testStubUrl = "testIIStubUrl"
  val testStubUri = "testIIStubUri"

  class Setup(unStubbed: Boolean) extends CodeMocks {
    val connector = new IncorporationInformationConnector {
      val stubUrl                 = testStubUrl
      val stubUri                 = testStubUri
      val incorpInfoUrl           = testUrl
      val incorpInfoUri           = testUri
      override val http : WSHttp  = mockWSHttp
      override val metricsService = new MockMetrics
      override val successCounter = metricsService.companyDetailsSuccessResponseCounter
      override val failedCounter  = metricsService.companyDetailsFailedResponseCounter
      override def timer          = metricsService.incorpInfoResponseTimer.time()
    }
  }


  "getCoHoCompanyDetails" should {
    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Fixtures.validCoHoCompanyDetailsResponse)

      await(connector.getCoHoCompanyDetails("testRegID", "testTxID")) mustBe IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup(true) {
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))

      await(connector.getCoHoCompanyDetails("testRegID", "testTxID")) mustBe IncorpInfoBadRequestResponse
    }

    "return a CoHo NotFound api response object for a bad request" in new Setup(true) {
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.failed(new NotFoundException("tstException")))

      await(connector.getCoHoCompanyDetails("testRegID", "testTxID")) mustBe IncorpInfoNotFoundResponse
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.failed(ex))

      await(connector.getCoHoCompanyDetails("testRegID", "testTxID")) mustBe IncorpInfoErrorResponse(ex)
    }
  }

  "getOfficerList" should {

    val tstOfficerList = OfficerList(
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


    val tstOfficerListObject = Json.parse(tstOfficerListJson).as[JsObject]
    val testTransId = "testTransId"
    val testRegId = "regId"

    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.successful(tstOfficerListObject))

      await(connector.getOfficerList(testTransId,testRegId)) mustBe tstOfficerList
    }

    "return an OfficerListNotFound exception when CoHo api response object returns an empty list" in new Setup(true) {
      val emptyOfficersListJson = JsObject(Seq("officers" -> Json.arr()))
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.successful(emptyOfficersListJson))

      intercept[OfficerListNotFoundException](await(connector.getOfficerList(testTransId,testRegId)))
    }

    "return an OfficerListNotFound exception for a downstream not found error" in new Setup(true) {
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(new NotFoundException("tstException")))

      intercept[OfficerListNotFoundException](await(connector.getOfficerList(testTransId,testRegId)))
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup(true) {
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getOfficerList(testTransId,testRegId)))
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getOfficerList(testTransId,testRegId)) )
    }
  }
}
