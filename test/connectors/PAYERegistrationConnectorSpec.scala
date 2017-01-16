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

import enums.DownstreamOutcome
import fixtures.PAYERegistrationFixture
import models.api.{CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import play.mvc.Http.Status
import testHelpers.PAYERegSpec
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
      mockHttpFailedPATCH[String, PAYERegistrationAPI]("tst-url", new BadRequestException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationBadRequestResponse
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, PAYERegistrationAPI]("tst-url", new ForbiddenException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedPATCH[String, PAYERegistrationAPI]("tst-url", e)

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return the correct PAYEResponse when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpPATCH[String, PAYERegistrationAPI]("tst-url", validPAYERegistrationAPI)

      await(connector.createNewRegistration("tstID")) shouldBe PAYERegistrationSuccessResponse(validPAYERegistrationAPI)
    }
  }

  "Calling getCompanyDetails" should {
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("tst-url", new BadRequestException("tst"))

      await(connector.getCompanyDetails("tstID")) shouldBe PAYERegistrationBadRequestResponse
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("tst-url", new ForbiddenException("tst"))

      await(connector.getCompanyDetails("tstID")) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedGET[CompanyDetailsAPI]("tst-url", e)

      await(connector.getCompanyDetails("tstID")) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return the correct PAYEResponse when the microservice returns a Company Details API model" in new Setup {
      mockHttpGet[CompanyDetailsAPI]("tst-url", validCompanyDetailsAPI)

      await(connector.getCompanyDetails("tstID")) shouldBe PAYERegistrationSuccessResponse(validCompanyDetailsAPI)
    }
    "return a Not Found PAYEResponse when the microservice returns no Company Details API model" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("tst-url", new NotFoundException("tst"))

      await(connector.getCompanyDetails("tstID")) shouldBe PAYERegistrationNotFoundResponse
    }
  }

  "Calling addTestRegistration" should {
    "return a successful PAYEResponse when the test reg is successfully added" in new Setup {
      mockHttpPOST[PAYERegistrationAPI, PAYERegistrationAPI]("tst-url", validPAYERegistrationAPI)

      await(connector.addTestRegistration(validPAYERegistrationAPI)) shouldBe PAYERegistrationSuccessResponse(validPAYERegistrationAPI)
    }

    "return a PAYE ErrorResponse when adding the test reg throws an exception" in new Setup {
      val e = new RuntimeException("tst")
      mockHttpFailedPOST[PAYERegistrationAPI, PAYERegistrationAPI]("tst-url", e)

      await(connector.addTestRegistration(validPAYERegistrationAPI)) shouldBe PAYERegistrationErrorResponse(e)
    }
  }

  "Calling testRegistrationTeardown" should {
    "return a successful outcome for a successful teardown" in new Setup {
      mockHttpGet[HttpResponse]("tst-url", HttpResponse(Status.OK))

      await(connector.testRegistrationTeardown()) shouldBe DownstreamOutcome.Success
    }
    "return a failed outcome for an unsuccessful teardown" in new Setup {
      val e = new RuntimeException("tst")
      mockHttpFailedGET[HttpResponse]("tst-url", e)

      await(connector.testRegistrationTeardown()) shouldBe DownstreamOutcome.Failure
    }
  }
}
