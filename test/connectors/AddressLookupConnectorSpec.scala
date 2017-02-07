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

import fixtures.BusinessRegistrationFixture
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException, Upstream4xxResponse}

import scala.concurrent.Future

class AddressLookupConnectorSpec extends PAYERegSpec with BusinessRegistrationFixture {

  trait Setup {
    val connector = new AddressLookupConnector {
      override val addressLookupFrontendUrl = "testBusinessRegUrl"
      override val http = mockWSHttp
    }
  }

  implicit val hc = HeaderCarrier()

  val testAddress = Json.obj("x"->"y")

  "getAddress" should {
    "return an address response" in new Setup {
      when(mockWSHttp.GET[JsObject](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(testAddress))

      await(connector.getAddress("123")) shouldBe testAddress
    }

    "return a Not Found response" in new Setup {
      when(mockWSHttp.GET[JsObject](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      intercept[NotFoundException](await(connector.getAddress("123")))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[JsObject](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException](await(connector.getAddress("321")))
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[JsObject](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new IndexOutOfBoundsException("other exception")))

      intercept[IndexOutOfBoundsException](await(connector.getAddress("321")))
    }
  }
}
