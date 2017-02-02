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

package connectors.test

import fixtures.CoHoAPIFixture
import play.api.libs.json.JsValue
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}

import scala.concurrent.Future

class TestCoHoAPIConnectorSpec extends PAYERegSpec with CoHoAPIFixture {

  val testUrl = "testCohoAPIUrl"
  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new TestCoHoAPIConnector {
      override val coHoAPIUrl = testUrl
      override val http = mockWSHttp
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
