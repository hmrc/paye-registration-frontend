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

import fixtures.PAYERegistrationFixture
import helpers.PAYERegSpec
import models.payeRegistration.PAYERegistration
import uk.gov.hmrc.play.http._

class PAYERegistrationConnectorSpec extends PAYERegSpec with PAYERegistrationFixture {

  class Setup {
    val connector = new PAYERegistrationConnector {
      override val payeRegUrl: String = "tst-url"
      override val http: HttpGet with HttpPost with HttpPatch = mockWSHttp
    }
  }

  implicit val hc = HeaderCarrier()

  "Calling createNewRegistration" should {
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, PAYERegistration]("tst-url", new BadRequestException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationBadRequestResponse
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, PAYERegistration]("tst-url", new ForbiddenException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedPATCH[String, PAYERegistration]("tst-url", e)

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return the correct PAYEResponse when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpPATCH[String, PAYERegistration]("tst-url", validPAYERegistration)

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationSuccessResponse(validPAYERegistration)
    }
  }


  "Calling getCurrentRegistration" should {
    "return the correct PAYEResponse when a Not Found response is returned by the microservice" in new Setup {
      mockHttpFailedGet[PAYERegistration]("tst-url", new NotFoundException("tst"))

      await(connector.getCurrentRegistration("tstID")) shouldBe PAYERegistrationNotFoundResponse
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGet[PAYERegistration]("tst-url", new ForbiddenException("tst"))

      await(connector.getCurrentRegistration("tstID")) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedGet[PAYERegistration]("tst-url", e)

      await(connector.getCurrentRegistration("tstID")) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return the correct PAYEResponse when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpGet[PAYERegistration]("tst-url", validPAYERegistration)

      await(connector.getCurrentRegistration("tstID")) shouldBe PAYERegistrationSuccessResponse(validPAYERegistration)
    }
  }
}
