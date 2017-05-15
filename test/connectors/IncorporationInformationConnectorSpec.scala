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

package connectors

import fixtures.CoHoAPIFixture
import mocks.MockMetrics
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier, NotFoundException}
import utils.PAYEFeatureSwitch

import scala.concurrent.Future

class IncorporationInformationConnectorSpec extends PAYERegSpec with CoHoAPIFixture {

  val testUrl = "testIIUrl"
  val testUri = "testIIUri"
  val testStubUrl = "testIIStubUrl"
  val testStubUri = "testIIStubUri"
  implicit val hc = HeaderCarrier()
  val mockFeatureSwitch = mock[PAYEFeatureSwitch]

  class Setup(unStubbed: Boolean) {
    val connector = new IncorporationInformationConnect {
      val stubUrl = testStubUrl
      val stubUri = testStubUri
      val incorpInfoUrl = testUrl
      val incorpInfoUri = testUri
      override val http : WSHttp = mockWSHttp
      override val metricsService = new MockMetrics
    }
  }


  "getCoHoCompanyDetails" should {
    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.successful(validCoHoCompanyDetailsResponse))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe IncorpInfoSuccessResponse(validCoHoCompanyDetailsResponse)
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup(true) {
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe IncorpInfoBadRequestResponse
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[CoHoCompanyDetailsModel](connector.incorpInfoUrl, Future.failed(ex))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe IncorpInfoErrorResponse(ex)
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

    val testTransId = "testTransId"

    "return a successful CoHo api response object for valid data" in new Setup(true) {
      mockHttpGet[OfficerList](connector.incorpInfoUrl, Future.successful(tstOfficerList))

      await(connector.getOfficerList(testTransId)) shouldBe tstOfficerList
    }

    "return a successful empty CoHo api response object for a not found request" in new Setup(true) {
      mockHttpGet[OfficerList](connector.incorpInfoUrl, Future.failed(new NotFoundException("tstException")))

      await(connector.getOfficerList(testTransId)) shouldBe OfficerList(items = Nil)
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup(true) {
      mockHttpGet[OfficerList](connector.incorpInfoUrl, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getOfficerList(testTransId)))
    }

    "return a CoHo error api response object for a downstream error" in new Setup(true) {
      val ex = new RuntimeException("tstException")
      mockHttpGet[OfficerList](connector.incorpInfoUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getOfficerList(testTransId)) )
    }
  }

}
