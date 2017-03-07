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
import models.api.{Director, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, Employment => EmploymentAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.http.Status
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

class PAYERegistrationConnectorSpec extends PAYERegSpec with PAYERegistrationFixture {

  class Setup {
    val connector = new PAYERegistrationConnector {
      override val payeRegUrl: String = "tst-url"
      override val http: WSHttp = mockWSHttp
    }
  }

  implicit val hc = HeaderCarrier()

  val badRequest = new BadRequestException("400")
  val forbidden = Upstream4xxResponse("403", 403, 403)
  val upstream4xx = Upstream4xxResponse("418", 418, 418)
  val upstream5xx = Upstream5xxResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val internalServiceException = new InternalServerException("502")

  "Calling createNewRegistration" should {
    "return a successful outcome when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpPATCH[String, HttpResponse]("tst-url", HttpResponse(Status.OK))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Success
    }
    "return a failed outcome when the microservice returns a 2xx response other than OK" in new Setup {
      mockHttpPATCH[String, HttpResponse]("tst-url", HttpResponse(Status.ACCEPTED))

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a Bad Request response" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", badRequest)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a Forbidden response" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", forbidden)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return an Upstream4xxResponse" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", upstream4xx)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return Upstream5xxResponse" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", upstream5xx)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
    "return a Internal Server Error" in new Setup {
      mockHttpFailedPATCH[String, HttpResponse]("tst-url", internalServiceException)

      await(connector.createNewRegistration("tstID")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getRegistration" should {
    "return the correct PAYEResponse when the microservice returns a PAYE Registration model" in new Setup {
      mockHttpGet[PAYERegistrationAPI]("tst-url", validPAYERegistrationAPI)

      await(connector.getRegistration("tstID")) shouldBe validPAYERegistrationAPI
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getRegistration("tstID")))
    }
    "return the correct PAYEResponse when a Not Found response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("test-url", notFound)

      intercept[NotFoundException](await(connector.getRegistration("tstID")))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getRegistration("tstID")))
    }
  }

  "Calling getCompanyDetails" should {
    "return the correct PAYEResponse when the microservice returns a Company Details API model" in new Setup {
      mockHttpGet[CompanyDetailsAPI]("tst-url", validCompanyDetailsAPI)

      await(connector.getCompanyDetails("tstID")) shouldBe Some(validCompanyDetailsAPI)
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getCompanyDetails("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getCompanyDetails("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Company Details API model" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("test-url", notFound)

      await(connector.getCompanyDetails("tstID")) shouldBe None
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[CompanyDetailsAPI]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getCompanyDetails("tstID")))
    }
  }

  "Calling upsertCompanyDetails" should {
    "return the correct PAYEResponse when the microservice completes and returns a Company Details API model" in new Setup {
      mockHttpPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", validCompanyDetailsAPI)

      await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)) shouldBe validCompanyDetailsAPI
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertCompanyDetails("tstID", validCompanyDetailsAPI)))
    }
  }

  "Calling getEmployment" should {
    "return the correct PAYEResponse when the microservice returns an Employment API model" in new Setup {
      mockHttpGet[EmploymentAPI]("tst-url", validEmploymentAPI)

      await(connector.getEmployment("tstID")) shouldBe Some(validEmploymentAPI)
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[EmploymentAPI]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getEmployment("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[EmploymentAPI]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getEmployment("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Employment API model" in new Setup {
      mockHttpFailedGET[EmploymentAPI]("tst-url", notFound)

      await(connector.getEmployment("tstID")) shouldBe None
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[EmploymentAPI]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getEmployment("tstID")))
    }
  }

  "Calling upsertEmployment" should {
    "return the correct PAYEResponse when the microservice completes and returns an Employment API model" in new Setup {
      mockHttpPATCH[EmploymentAPI, EmploymentAPI]("tst-url", validEmploymentAPI)

      await(connector.upsertEmployment("tstID", validEmploymentAPI)) shouldBe validEmploymentAPI
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[EmploymentAPI, EmploymentAPI]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertEmployment("tstID", validEmploymentAPI)))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[EmploymentAPI, EmploymentAPI]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertEmployment("tstID", validEmploymentAPI)))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[EmploymentAPI, EmploymentAPI]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertEmployment("tstID", validEmploymentAPI)))
    }
  }

  "Calling getDirectors" should {
    "return the correct PAYEResponse when the microservice returns a list of Directors" in new Setup {
      mockHttpGet[Seq[Director]]("tst-url", validDirectorList)

      await(connector.getDirectors("tstID")) shouldBe validDirectorList
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getDirectors("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getDirectors("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Employment API model" in new Setup {
      mockHttpFailedGET[Seq[Director]]("tst-url", notFound)

      await(connector.getDirectors("tstID")) shouldBe Seq.empty
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getDirectors("tstID")))
    }
  }

  "Calling upsertDirectors" should {
    "return the correct PAYEResponse when the microservice completes and returns a list of Directors" in new Setup {
      mockHttpPATCH[Seq[Director], Seq[Director]]("tst-url", validDirectorList)

      await(connector.upsertDirectors("tstID", validDirectorList)) shouldBe validDirectorList
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertDirectors("tstID", validDirectorList)))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertDirectors("tstID", validDirectorList)))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertDirectors("tstID", validDirectorList)))
    }
  }

  "Calling getSICCodes" should {
    "return the correct PAYEResponse when the microservice returns a list of Directors" in new Setup {
      mockHttpGet[Seq[SICCode]]("tst-url", validSICCodesList)

      await(connector.getDirectors("tstID")) shouldBe validSICCodesList
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getSICCodes("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getSICCodes("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Employment API model" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("tst-url", notFound)

      await(connector.getSICCodes("tstID")) shouldBe Seq.empty
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getSICCodes("tstID")))
    }
  }

  "Calling upsertSICCodes" should {
    "return the correct PAYEResponse when the microservice completes and returns a list of Directors" in new Setup {
      mockHttpPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", validSICCodesList)

      await(connector.upsertSICCodes("tstID", validSICCodesList)) shouldBe validSICCodesList
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertSICCodes("tstID", validSICCodesList)))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertSICCodes("tstID", validSICCodesList)))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertSICCodes("tstID", validSICCodesList)))
    }
  }

  "Calling getPAYEContact" should {
    "return the correct PAYEResponse when the microservice returns a PAYE Contact model" in new Setup {
      mockHttpGet[PAYEContact]("tst-url", validPAYEContactAPI)

      await(connector.getPAYEContact("tstID")) shouldBe Some(validPAYEContactAPI)
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYEContact]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getPAYEContact("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYEContact]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getPAYEContact("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Company Details API model" in new Setup {
      mockHttpFailedGET[PAYEContact]("test-url", notFound)

      await(connector.getPAYEContact("tstID")) shouldBe None
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYEContact]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getPAYEContact("tstID")))
    }
  }

  "Calling upsertPAYEContact" should {
    "return the correct PAYEResponse when the microservice completes and returns a PAYE Contact model" in new Setup {
      mockHttpPATCH[PAYEContact, PAYEContact]("tst-url", validPAYEContactAPI)

      await(connector.upsertPAYEContact("tstID", validPAYEContactAPI)) shouldBe validPAYEContactAPI
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertPAYEContact("tstID", validPAYEContactAPI)))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertPAYEContact("tstID", validPAYEContactAPI)))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertPAYEContact("tstID", validPAYEContactAPI)))
    }
  }

  "Calling getCompletionCapacity" should {
    "return the correct PAYEResponse when the microservice returns a PAYE Contact model" in new Setup {
      mockHttpGet[String]("tst-url", "tst")

      await(connector.getCompletionCapacity("tstID")) shouldBe Some("tst")
    }
    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[String]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getCompletionCapacity("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[String]("test-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.getCompletionCapacity("tstID")))
    }
    "return a Not Found PAYEResponse when the microservice returns no Completion Capacity String" in new Setup {
      mockHttpFailedGET[String]("test-url", notFound)

      await(connector.getCompletionCapacity("tstID")) shouldBe None
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[String]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getCompletionCapacity("tstID")))
    }
  }

  "Calling upsertCompletionCapacity" should {
    "return the correct PAYEResponse when the microservice completes and returns a Completion Capacity String" in new Setup {
      mockHttpPATCH[String, String]("tst-url", "tst")

      await(connector.upsertCompletionCapacity("tstID", "tst")) shouldBe "tst"
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, String]("tst-url", forbidden)

      intercept[Upstream4xxResponse](await(connector.upsertCompletionCapacity("tstID", "tst")))
    }
    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[String, String]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertCompletionCapacity("tstID", "tst")))
    }
    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, String]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertCompletionCapacity("tstID", "tst")))
    }
  }
}