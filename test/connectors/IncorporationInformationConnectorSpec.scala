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

import common.exceptions.DownstreamExceptions
import enums.IncorporationStatus
import helpers.mocks.MockMetrics
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, InternalServerException}

import java.time.LocalDate
import scala.concurrent.Future

class IncorporationInformationConnectorSpec extends PayeComponentSpec with PayeFakedApp {

  implicit val request: FakeRequest[_] = FakeRequest()

  class Setup(unStubbed: Boolean = true) extends CodeMocks {
    val connector = new IncorporationInformationConnector(
      new MockMetrics,
      mockHttpClient
    )(injAppConfig, scala.concurrent.ExecutionContext.Implicits.global)
  }

  "setupSubscription" should {

    "return Some(IncorporationStatus.Value) when II returns a 200" in new Setup {

      mockHttpPOST[JsObject, Option[IncorporationStatus.Value]]("", Some(IncorporationStatus.accepted))

      await(connector.setupSubscription("fooTxID", "barSubscriber")) mustBe Some(IncorporationStatus.accepted)
    }

    "return an Exception when something goes wrong whilst calling ii" in new Setup {
      mockHttpFailedPOST[JsObject, Option[IncorporationStatus.Value]]("", new BadRequestException("foo"))

      intercept[BadRequestException](await(connector.setupSubscription("foo", "bar")))
    }
  }

  "cancelSubscription" should {
    "return true if an OK is returned from ii" in new Setup {
      mockHttpDelete[Boolean](true)
      await(connector.cancelSubscription("tx-12345", "12345")) mustBe true
    }

    "return false if an Intrernal Server Exception is returned from ii" in new Setup {
      mockHttpFailedDelete[Boolean](new InternalServerException("Internal Server Exception"))
      await(connector.cancelSubscription("tx-12345", "12345")) mustBe false
    }
  }

  "getCoHoCompanyDetails" should {
    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[IncorpInfoResponse](connector.incorpInfoUrl, IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse))

      await(connector.getCoHoCompanyDetails("testRegID", "testTxID")) mustBe IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)
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

    val incorpDate = LocalDate.of(2018,5,5)

    "return incorporationDate when successful" in new Setup(true) {
      mockHttpGet[Option[LocalDate]](connector.incorpInfoUrl, Future.successful(Some(incorpDate)))
      await(connector.getIncorporationInfoDate(testRegId, testTransId)) mustBe Some(incorpDate)
    }

    "return an IncorporationInformationResponseException when unexpected failed future occurs" in new Setup(true) {
      mockHttpGet[Option[LocalDate]](connector.incorpInfoUrl, Future.failed(new DownstreamExceptions.IncorporationInformationResponseException("tstException")))
      intercept[DownstreamExceptions.IncorporationInformationResponseException](await(connector.getIncorporationInfoDate(testRegId, testTransId)))
    }
  }

  "getOfficerList" should {

    val tstOfficerList = OfficerList(
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

    val testTransId = "testTransId"
    val testRegId = "regId"

    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[OfficerList](connector.incorpInfoUrl, tstOfficerList)

      await(connector.getOfficerList(testTransId, testRegId)) mustBe tstOfficerList
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[JsObject](connector.incorpInfoUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getOfficerList(testTransId, testRegId)))
    }
  }
}