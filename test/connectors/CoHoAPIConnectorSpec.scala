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
import models.api.Name
import models.external.{CHROAddress, CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.libs.json.{JsValue, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}

import scala.concurrent.Future

class CoHoAPIConnectorSpec extends PAYERegSpec with CoHoAPIFixture {

  val testUrl = "testCohoAPIUrl"
  val testUri = "testCohoAPIUri"
  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new CoHoAPIConnect {
      val coHoAPIUrl = testUrl
      val coHoAPIUri = testUri
      override val http : WSHttp = mockWSHttp
    }
  }


  "getCoHoCompanyDetails" should {
    "return a successful CoHo api response object for valid data" in new Setup {
      mockHttpGet[CoHoCompanyDetailsModel](connector.coHoAPIUrl, Future.successful(validCoHoCompanyDetailsResponse))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe CohoApiSuccessResponse(validCoHoCompanyDetailsResponse)
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup {
      mockHttpGet[CoHoCompanyDetailsModel](connector.coHoAPIUrl, Future.failed(new BadRequestException("tstException")))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe CohoApiBadRequestResponse
    }

    "return a CoHo error api response object for a downstream error" in new Setup {
      val ex = new RuntimeException("tstException")
      mockHttpGet[CoHoCompanyDetailsModel](connector.coHoAPIUrl, Future.failed(ex))

      await(connector.getCoHoCompanyDetails("testRegID")) shouldBe CohoApiErrorResponse(ex)
    }
  }

  "getRegisteredOfficeAddress" should {

    val testAddr =
      CHROAddress(
        "premises",
        "l1",
        Some("l2"),
        "locality",
        Some("country"),
        Some("pobox"),
        Some("pCode"),
        Some("region")
      )

    val testTransId = "testTransId"

    "return a successful CoHo api response object for valid data" in new Setup {
      mockHttpGet[CHROAddress](connector.coHoAPIUrl, Future.successful(testAddr))

      await(connector.getRegisteredOfficeAddress(testTransId)) shouldBe testAddr
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup {
      mockHttpGet[CHROAddress](connector.coHoAPIUrl, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getRegisteredOfficeAddress(testTransId)))
    }

    "return a CoHo error api response object for a downstream error" in new Setup {
      val ex = new RuntimeException("tstException")
      mockHttpGet[CHROAddress](connector.coHoAPIUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getRegisteredOfficeAddress(testTransId)) )
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

    "return a successful CoHo api response object for valid data" in new Setup {
      mockHttpGet[OfficerList](connector.coHoAPIUrl, Future.successful(tstOfficerList))

      await(connector.getOfficerList(testTransId)) shouldBe tstOfficerList
    }

    "return a successful empty CoHo api response object for a not found request" in new Setup {
      mockHttpGet[OfficerList](connector.coHoAPIUrl, Future.failed(new NotFoundException("tstException")))

      await(connector.getOfficerList(testTransId)) shouldBe OfficerList(items = Nil)
    }

    "return a CoHo Bad Request api response object for a bad request" in new Setup {
      mockHttpGet[OfficerList](connector.coHoAPIUrl, Future.failed(new BadRequestException("tstException")))

      intercept[BadRequestException](await(connector.getOfficerList(testTransId)))
    }

    "return a CoHo error api response object for a downstream error" in new Setup {
      val ex = new RuntimeException("tstException")
      mockHttpGet[OfficerList](connector.coHoAPIUrl, Future.failed(ex))

      intercept[RuntimeException](await(connector.getOfficerList(testTransId)) )
    }
  }

}
