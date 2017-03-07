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
import models.external.CurrentProfile
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends PAYERegSpec with BusinessRegistrationFixture {

  val mockBusRegConnector = mock[BusinessRegistrationConnector]

  trait Setup {
    val connector = new BusinessRegistrationConnector {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http = mockWSHttp
    }
  }

  implicit val hc = HeaderCarrier()

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      mockHttpGet[CurrentProfile]("testUrl", validBusinessRegistrationResponse)

      await(connector.retrieveCurrentProfile) shouldBe BusinessRegistrationSuccessResponse(validBusinessRegistrationResponse)
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockWSHttp.GET[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      await(connector.retrieveCurrentProfile) shouldBe BusinessRegistrationNotFoundResponse
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockWSHttp.GET[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      await(connector.retrieveCurrentProfile) shouldBe BusinessRegistrationForbiddenResponse
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      when(mockWSHttp.GET[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new Exception("exception")))

      await(connector.retrieveCurrentProfile).getClass shouldBe BusinessRegistrationErrorResponse(new Exception).getClass
    }
  }
}
