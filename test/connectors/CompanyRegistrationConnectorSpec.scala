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

import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class CompanyRegistrationConnectorSpec extends PAYERegSpec {

  class Setup {
    val connector = new CompanyRegistrationConnect {
      val companyRegistrationUri = "testUrl"
      val http = mockWSHttp
    }

    implicit val hc = HeaderCarrier()
  }

  "getTransactionId" should {
    "return a transaction id" in new Setup {
      mockHttpGet[String](connector.companyRegistrationUri, Future.successful("testTransactionId"))

      val result = await(connector.getTransactionId("testRegId"))
      result shouldBe "testTransactionId"
    }

    "throw a bad request exception" in new Setup {
      mockHttpGet[String](connector.companyRegistrationUri, Future.failed(new BadRequestException("tstException")))

        intercept[BadRequestException](await(connector.getTransactionId("testRegId")))
    }

    "throw a RuntimeException" in new Setup {
      mockHttpGet[String](connector.companyRegistrationUri, Future.failed(new RuntimeException))

      intercept[RuntimeException](await(connector.getTransactionId("testRegId")))
    }
  }
}
