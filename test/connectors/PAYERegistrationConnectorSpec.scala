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
import play.api.http.Status
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
    "return a failed outcome when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", new BadRequestException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a failed outcome when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", new ForbiddenException("tst"))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a failed outcome when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", e)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a failed outcome when the microservice returns a 2xx response other than OK" in new Setup {
      mockHttpPATCH[String, HttpResponse]("tst-url", HttpResponse(Status.ACCEPTED))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a successful outcome when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpPATCH[String, HttpResponse]("tst-url", HttpResponse(Status.OK))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling getRegistration" should {
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("tst-url", new ForbiddenException("tst"))

      await(connector.getRegistration("tstID")) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when a Not Found response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("tst-url", new NotFoundException("tst"))

      await(connector.getRegistration("tstID")) shouldBe PAYERegistrationNotFoundResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedGET[PAYERegistrationAPI]("tst-url", e)

      await(connector.getRegistration("tstID")) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return the correct PAYEResponse when the microservice returns a PAYE Registration model" in new Setup {
      mockHttpGet[PAYERegistrationAPI]("tst-url", validPAYERegistrationAPI)

      await(connector.getRegistration("tstID")) shouldBe PAYERegistrationSuccessResponse(validPAYERegistrationAPI)
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

  "Calling upsertCompanyDetails" should {
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", new ForbiddenException("tst"))

      await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)) shouldBe PAYERegistrationForbiddenResponse
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      val e = new InternalServerException("tst")
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", e)

      await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)) shouldBe PAYERegistrationErrorResponse(e)
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", new NotFoundException("tst"))

      await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)) shouldBe PAYERegistrationNotFoundResponse
    }
    "return the correct PAYEResponse when the microservice completes and returns a Company Details API model" in new Setup {
      mockHttpPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", validCompanyDetailsAPI)

      await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)) shouldBe PAYERegistrationSuccessResponse(validCompanyDetailsAPI)
    }
  }
}
