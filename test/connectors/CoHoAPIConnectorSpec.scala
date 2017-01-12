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
import models.externalAPIModels.coHo.CoHoCompanyDetailsModel
import play.api.libs.json.JsValue
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{HttpResponse, BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class CoHoAPIConnectorSpec extends PAYERegSpec with CoHoAPIFixture {

  val testUrl = "testCohoAPIUrl"
  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new CoHoAPIConnector {
      val coHoAPIUrl = testUrl
      val http = mockWSHttp
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

  "addCoHoCompanyDetails" should {
    "return a valid response when successfully set up" in new Setup {
      val resp = HttpResponse(responseStatus = 200)
      mockHttpPOST[JsValue, HttpResponse](connector.coHoAPIUrl, Future.successful(resp))

      await(connector.addCoHoCompanyDetails(validCoHoCompanyDetailsResponse)) shouldBe resp
    }
  }

  "tearDownCoHoCompanyDetails" should {
    "return a valid response when successfully completed" in new Setup {
      val resp = HttpResponse(responseStatus = 200)
      mockHttpGet[HttpResponse](connector.coHoAPIUrl, Future.successful(resp))

      await(connector.tearDownCoHoCompanyDetails()) shouldBe resp
    }
  }

}
