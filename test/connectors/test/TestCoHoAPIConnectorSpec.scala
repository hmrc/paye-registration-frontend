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

package connectors.test

import config.AppConfig
import helpers.PayeComponentSpec
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext

class TestCoHoAPIConnectorSpec extends PayeComponentSpec {

  val testUrl = "testIncorpInfoUrl"
  val config: AppConfig = app.injector.instanceOf[AppConfig]


  class Setup extends CodeMocks {
    val testConnector: TestIncorpInfoConnector = new TestIncorpInfoConnector(
      mockHttpClient,
      config,
      ec
    ) {
      override val incorpFEStubsUrl: String = testUrl
      override val incorpInfoUrl: String = testUrl
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  "setupCoHoCompanyDetails" should {
    "return a valid response when successfully set up" in new Setup {
      val resp: HttpResponse = HttpResponse(200, "")

      mockHttpPOST[JsValue, HttpResponse](testConnector.incorpFEStubsUrl, resp)

      await(testConnector.setupCoHoCompanyDetails("123", "company name")) mustBe resp
    }
  }

  "teardownCoHoCompanyDetails" should {
    "return a valid response when successfully set up" in new Setup {
      val resp: HttpResponse = HttpResponse(200, "")

      mockHttpPUT[String, HttpResponse](testConnector.incorpFEStubsUrl, resp)

      await(testConnector.teardownCoHoCompanyDetails()) mustBe resp
    }
  }
}
