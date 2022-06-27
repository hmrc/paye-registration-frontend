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

import common.exceptions.DownstreamExceptions.{IncorporationInformationResponseException, OfficerListNotFoundException}
import config.AppConfig
import enums.IncorporationStatus
import helpers.mocks.MockMetrics
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import uk.gov.hmrc.http.{BadRequestException, HttpClient, HttpResponse, InternalServerException, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class IncorporationInformationConnectorSpec extends PayeComponentSpec with PayeFakedApp {

  val testUrl = "testIIUrl"
  val testUri = "testIIUri"
  val testStubUrl = "testIIStubUrl"
  val testStubUri = "testIIStubUri"
  val testPayeRegFeUrl = "http://paye-fe"

  class Setup(unStubbed: Boolean = true) extends CodeMocks {
    val connector = new IncorporationInformationConnector {
      val stubUrl = testStubUrl
      val stubUri = testStubUri
      val incorpInfoUrl = testUrl
      val incorpInfoUri = testUri
      val payeRegFeUrl = testPayeRegFeUrl
      override val http: HttpClient = mockHttpClient
      override val metricsService = new MockMetrics
      override val successCounter = metricsService.companyDetailsSuccessResponseCounter
      override val failedCounter = metricsService.companyDetailsFailedResponseCounter

      override def timer = metricsService.incorpInfoResponseTimer.time()

      override implicit val appConfig: AppConfig = mockAppConfig
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  "setupSubscription" should {
    val responseJson = Json.parse(
      s"""
         |{
         | "SCRSIncorpStatus": {
         |   "IncorpSubscriptionKey" : {
         |     "transactionId" : "fooTxID",
         |     "subscriber"    : "SCRS",
         |     "discriminator" : "paye-fe"
         |   },
         |   "IncorpStatusEvent": {
         |     "status" : "accepted",
         |     "crn" : "12345678",
         |     "description" : "test desc"
         |   }
         | }
         |}
      """.stripMargin)
    "return Some(IncorporationStatus.Value) when II returns a 200" in new Setup {
      val httpResponse = HttpResponse(200, Some(responseJson))
      mockHttpPOST[JsObject, HttpResponse]("", httpResponse)

      await(connector.setupSubscription("fooTxID", "barSubscriber")) mustBe Some(IncorporationStatus.accepted)
    }
    "return JsResultException when subscriber does not match with the one returned from II" in new Setup {
      val httpResponse = HttpResponse(200, Some(responseJson))
      mockHttpPOST[JsObject, HttpResponse]("", httpResponse)

      intercept[JsResultException](await(connector.setupSubscription("fooTxID", "bar", subscriber = "fooBarWillNotMatch")))
    }

    "return None when II returns a 202" in new Setup {
      val httpResponse = HttpResponse(202, Some(responseJson))
      mockHttpPOST[JsObject, HttpResponse]("", httpResponse)

      await(connector.setupSubscription("foo", "bar")) mustBe None
    }

    "return IncorporationInformationResponseException when II returns any other status than 200 / 202 but still a success response" in new Setup {
      val httpResponse = HttpResponse(203, Some(responseJson))
      mockHttpPOST[JsObject, HttpResponse]("", httpResponse)

      intercept[IncorporationInformationResponseException](await(connector.setupSubscription("foo", "bar")))
    }
    "return an JsResultException when json cannot be parsed for a 200 from II" in new Setup {
      val httpResponse = HttpResponse(200, Some(Json.obj("foo" -> "bar")))
      mockHttpPOST[JsObject, HttpResponse]("", httpResponse)

      intercept[JsResultException](await(connector.setupSubscription("fooTxID", "barSubscriber")))
    }
    "return an Exception when something goes wrong whilst calling ii" in new Setup {
      mockHttpFailedPOST[JsObject, HttpResponse]("", new BadRequestException("foo"))

      intercept[BadRequestException](await(connector.setupSubscription("foo", "bar")))
    }
  }

  "cancelSubscription" should {
    "return true if an OK is returned from ii" in new Setup {
      mockHttpDelete[HttpResponse](HttpResponse(200))
      await(connector.cancelSubscription("tx-12345", "12345")) mustBe true
    }
    "return true if a NotFound is returned from ii" in new Setup {
      mockHttpFailedDelete[HttpResponse](new NotFoundException("Not Found"))
      await(connector.cancelSubscription("tx-12345", "12345")) mustBe true
    }
    "return false if an Intrernal Server Exception is returned from ii" in new Setup {
      mockHttpFailedDelete[HttpResponse](new InternalServerException("Internal Server Exception"))
      await(connector.cancelSubscription("tx-12345", "12345")) mustBe false
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

  "getIncorporationData" should {

    val testTransId = "testTransId"
    val testRegId = "regId"

    val testJsonWithDate = Json.parse(
      """
        |{
        |   "crn" : "some-crn",
        |   "incorporationDate" : "2018-05-05"
        |}
      """.stripMargin)

    "return an Incorp Info JsValue" in new Setup(true) {
      mockHttpGet[HttpResponse](connector.incorpInfoUrl, Future.successful(HttpResponse(200, Some(testJsonWithDate))))
      await(connector.getIncorporationInfo(testRegId, testTransId)) mustBe testJsonWithDate
    }

    "return an empty json" in new Setup(true) {
      mockHttpGet[HttpResponse](connector.incorpInfoUrl, Future.successful(HttpResponse(204)))
      await(connector.getIncorporationInfo(testRegId, testTransId)) mustBe Json.obj()
    }

    "return a BadRequest for a bad request" in new Setup(true) {
      mockHttpGet[JsValue](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))
      intercept[InternalServerException](await(connector.getIncorporationInfo(testRegId, testTransId)))
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

      await(connector.getOfficerList(testTransId, testRegId)) mustBe tstOfficerList
    }

    "return an OfficerListNotFound exception when CoHo api response object returns an empty list" in new Setup(true) {
      val emptyOfficersListJson = JsObject(Seq("officers" -> Json.arr()))
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.successful(emptyOfficersListJson))

      intercept[OfficerListNotFoundException](await(connector.getOfficerList(testTransId, testRegId)))
    }

    "return an OfficerListNotFound exception for a downstream not found error" in new Setup(true) {
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(new NotFoundException("tstException")))

      intercept[OfficerListNotFoundException](await(connector.getOfficerList(testTransId, testRegId)))
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup(true) {
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getOfficerList(testTransId, testRegId)))
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getOfficerList(testTransId, testRegId)))
    }
  }
}