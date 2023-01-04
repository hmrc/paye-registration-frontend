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

package connectors

import enums.{DownstreamOutcome, RegistrationDeletion}
import helpers.PayeComponentSpec
import helpers.mocks.MockMetrics
import models.api.{Director, Employment, PAYEContact, SICCode, CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Json
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext

class PAYERegistrationConnectorSpec extends PayeComponentSpec {

  class Setup extends CodeMocks {

    when(mockAppConfig.servicesConfig).thenReturn(mockServicesConfig)
    when(mockServicesConfig.baseUrl("paye-registration")).thenReturn("tst-url")

    val connector = new PAYERegistrationConnector(
      new MockMetrics,
      mockHttpClient,
      mockAppConfig
    )
  }

  val ok = HttpResponse(200, "")
  val accepted = HttpResponse(202, "")
  val noContent = HttpResponse(204, "")
  val badRequest = new BadRequestException("400")
  val forbidden = UpstreamErrorResponse("403", 403, 403)
  val upstream4xx = UpstreamErrorResponse("418", 418, 418)
  val upstream5xx = UpstreamErrorResponse("500", 500, 500)
  val notFound = new NotFoundException("404")
  val internalServiceException = new InternalServerException("502")

  "Calling createNewRegistration" should {
    "return a successful outcome when the microservice successfully creates a new PAYE Registration" in new Setup {
      mockHttpPATCH[String, DownstreamOutcome.Value]("tst-url", DownstreamOutcome.Success)

      await(connector.createNewRegistration("tstID", "tstTXID")) mustBe DownstreamOutcome.Success
    }

    "return a DownstreamOutcome.Failure on any Exception" in new Setup {
      mockHttpFailedPATCH[String, DownstreamOutcome.Value]("tst-url", internalServiceException)

      await(connector.createNewRegistration("tstID", "tstTXID")) mustBe DownstreamOutcome.Failure
    }
  }

  "Calling getRegistrationId" should {
    "return a valid regId" in new Setup {
      mockHttpGet[String]("test-url", "returnRegId")

      await(connector.getRegistrationId("testTxID")) mustBe "returnRegId"
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getRegistration("tstID")))
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

  "Calling getRegistration" should {
    "return the correct PAYEResponse when the microservice returns a PAYE Registration model" in new Setup {
      mockHttpGet[PAYERegistrationAPI]("tst-url", Fixtures.validPAYERegistrationAPI)

      await(connector.getRegistration("tstID")) mustBe Fixtures.validPAYERegistrationAPI
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[PAYERegistrationAPI]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getRegistration("tstID")))
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
      mockHttpGet[Option[CompanyDetailsAPI]]("tst-url", Some(Fixtures.validCompanyDetailsAPI))

      await(connector.getCompanyDetails("tstID")) mustBe Some(Fixtures.validCompanyDetailsAPI)
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[CompanyDetailsAPI]]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getCompanyDetails("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[CompanyDetailsAPI]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getCompanyDetails("tstID")))
    }

    "return a Not Found PAYEResponse when the microservice returns no Company Details API model" in new Setup {
      mockHttpGet[Option[CompanyDetailsAPI]]("test-url", None)

      await(connector.getCompanyDetails("tstID")) mustBe None
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[CompanyDetailsAPI]]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getCompanyDetails("tstID")))
    }
  }

  "Calling upsertCompanyDetails" should {
    "return the correct PAYEResponse when the microservice completes and returns a Company Details API model" in new Setup {
      mockHttpPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", Fixtures.validCompanyDetailsAPI)

      await(connector.upsertCompanyDetails("tstID", Fixtures.validCompanyDetailsAPI)) mustBe Fixtures.validCompanyDetailsAPI
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertCompanyDetails("tstID", Fixtures.validCompanyDetailsAPI)))
    }

    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertCompanyDetails("tstID", Fixtures.validCompanyDetailsAPI)))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[CompanyDetailsAPI, CompanyDetailsAPI]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertCompanyDetails("tstID", Fixtures.validCompanyDetailsAPI)))
    }
  }

  "Calling getEmployment" should {
    "return the correct PAYEResponse when the microservice returns an Employment API model" in new Setup {
      mockHttpGet[Option[Employment]]("tst-url", Some(Fixtures.validEmploymentApi))

      await(connector.getEmployment("tstID")) mustBe Some(Fixtures.validEmploymentApi)
    }

    "return a None when the microservice returns a 204 status code" in new Setup {
      mockHttpGet[Option[Employment]]("tst-url", None)

      await(connector.getEmployment("tstID")) mustBe None
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[Employment]]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getEmployment("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[Employment]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getEmployment("tstID")))
    }

    "return a exception PAYEResponse when the microservice returns no doc" in new Setup {
      mockHttpFailedGET[Option[Employment]]("tst-url", notFound)

      intercept[NotFoundException](await(connector.getEmployment("tstID")))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[Employment]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getEmployment("tstID")))
    }
  }

  "Calling upsertEmployment" should {
    "return the correct PAYEResponse when the microservice completes and returns an Employment API model" in new Setup {
      mockHttpPATCH[Employment, Employment]("tst-url", Fixtures.validEmploymentApi)

      await(connector.upsertEmployment("tstID", Fixtures.validEmploymentApi)) mustBe Fixtures.validEmploymentApi
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Employment, Employment]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertEmployment("tstID", Fixtures.validEmploymentApi)))
    }

    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[Employment, Employment]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertEmployment("tstID", Fixtures.validEmploymentApi)))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Employment, Employment]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertEmployment("tstID", Fixtures.validEmploymentApi)))
    }
  }

  "Calling getDirectors" should {
    "return the correct PAYEResponse when the microservice returns a list of Directors" in new Setup {
      mockHttpGet[Seq[Director]]("tst-url", Fixtures.validDirectorList)

      await(connector.getDirectors("tstID")) mustBe Fixtures.validDirectorList
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getDirectors("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getDirectors("tstID")))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[Director]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getDirectors("tstID")))
    }
  }

  "Calling upsertDirectors" should {
    "return the correct PAYEResponse when the microservice completes and returns a list of Directors" in new Setup {
      mockHttpPATCH[Seq[Director], Seq[Director]]("tst-url", Fixtures.validDirectorList)

      await(connector.upsertDirectors("tstID", Fixtures.validDirectorList)) mustBe Fixtures.validDirectorList
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertDirectors("tstID", Fixtures.validDirectorList)))
    }

    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertDirectors("tstID", Fixtures.validDirectorList)))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[Director], Seq[Director]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertDirectors("tstID", Fixtures.validDirectorList)))
    }
  }

  "Calling getSICCodes" should {
    "return the correct PAYEResponse when the microservice returns a list of Directors" in new Setup {
      mockHttpGet[Seq[SICCode]]("tst-url", Fixtures.validSICCodesList)

      await(connector.getDirectors("tstID")) mustBe Fixtures.validSICCodesList
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("tst-url", badRequest)

      intercept[BadRequestException](await(connector.getSICCodes("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getSICCodes("tstID")))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Seq[SICCode]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.getSICCodes("tstID")))
    }
  }

  "Calling upsertSICCodes" should {
    "return the correct PAYEResponse when the microservice completes and returns a list of Directors" in new Setup {
      mockHttpPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", Fixtures.validSICCodesList)

      await(connector.upsertSICCodes("tstID", Fixtures.validSICCodesList)) mustBe Fixtures.validSICCodesList
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertSICCodes("tstID", Fixtures.validSICCodesList)))
    }

    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertSICCodes("tstID", Fixtures.validSICCodesList)))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[Seq[SICCode], Seq[SICCode]]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertSICCodes("tstID", Fixtures.validSICCodesList)))
    }
  }

  "Calling getPAYEContact" should {
    "return the correct PAYEResponse when the microservice returns a PAYE Contact model" in new Setup {
      mockHttpGet[Option[PAYEContact]]("tst-url", Some(Fixtures.validPAYEContactAPI))

      await(connector.getPAYEContact("tstID")) mustBe Some(Fixtures.validPAYEContactAPI)
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[PAYEContact]]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getPAYEContact("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[PAYEContact]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getPAYEContact("tstID")))
    }

    "return a Not Found PAYEResponse when the microservice returns no Company Details API model" in new Setup {
      mockHttpGet[Option[PAYEContact]]("test-url", None)

      await(connector.getPAYEContact("tstID")) mustBe None
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[PAYEContact]]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getPAYEContact("tstID")))
    }
  }

  "Calling upsertPAYEContact" should {
    "return the correct PAYEResponse when the microservice completes and returns a PAYE Contact model" in new Setup {
      mockHttpPATCH[PAYEContact, PAYEContact]("tst-url", Fixtures.validPAYEContactAPI)

      await(connector.upsertPAYEContact("tstID", Fixtures.validPAYEContactAPI)) mustBe Fixtures.validPAYEContactAPI
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertPAYEContact("tstID", Fixtures.validPAYEContactAPI)))
    }

    "return a Not Found PAYEResponse when the microservice returns a NotFound response (No PAYERegistration in database)" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", notFound)

      intercept[NotFoundException](await(connector.upsertPAYEContact("tstID", Fixtures.validPAYEContactAPI)))
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[PAYEContact, PAYEContact]("tst-url", internalServiceException)

      intercept[InternalServerException](await(connector.upsertPAYEContact("tstID", Fixtures.validPAYEContactAPI)))
    }
  }

  "Calling getCompletionCapacity" should {
    "return the correct PAYEResponse when the microservice returns a Completion Capacity string" in new Setup {
      mockHttpGet[Option[String]]("tst-url", Some("tst"))

      await(connector.getCompletionCapacity("tstID")) mustBe Some("tst")
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getCompletionCapacity("tstID")))
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getCompletionCapacity("tstID")))
    }

    "return a Not Found PAYEResponse when the microservice returns no Completion Capacity String" in new Setup {
      mockHttpGet[Option[String]]("test-url", None)

      await(connector.getCompletionCapacity("tstID")) mustBe None
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getCompletionCapacity("tstID")))
    }
  }

  "Calling upsertCompletionCapacity" should {
    "return the correct PAYEResponse when the microservice completes and returns a Completion Capacity String" in new Setup {
      mockHttpPATCH[String, String]("tst-url", "tst")

      await(connector.upsertCompletionCapacity("tstID", "tst")) mustBe "tst"
    }

    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[String, String]("tst-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.upsertCompletionCapacity("tstID", "tst")))
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

  "Calling getAcknowledgementReference" should {
    "return the correct PAYEResponse when the microservice returns an acknowledgement reference" in new Setup {
      mockHttpGet[Option[String]]("tst-url", Some("tst"))

      await(connector.getAcknowledgementReference("tstID")) mustBe Some("tst")
    }

    "return the correct PAYEResponse when a Bad Request response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", badRequest)

      intercept[BadRequestException](await(connector.getAcknowledgementReference("tstID")))
    }
    "return the correct PAYEResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", forbidden)

      intercept[UpstreamErrorResponse](await(connector.getAcknowledgementReference("tstID")))
    }

    "return a Not Found PAYEResponse when the microservice returns no Eligibility response" in new Setup {
      mockHttpGet[Option[String]]("test-url", None)

      await(connector.getAcknowledgementReference("tstID")) mustBe None
    }

    "return the correct PAYEResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[Option[String]]("test-url", internalServiceException)

      intercept[InternalServerException](await(connector.getAcknowledgementReference("tstID")))
    }
  }

  "calling submitRegistration" should {
    "return a Success" in new Setup {
      mockHttpPUT[String, DESResponse]("test-url", Success)
      await(connector.submitRegistration("tstID")) mustBe Success
    }

    "return Failed when Exception thrown" in new Setup {
      mockHttpFailedPUT[String, DESResponse]("test-url", new Exception("foo"))

      await(connector.submitRegistration("tstID")) mustBe Failed
    }
  }

  "deleteRejectedRegistrationDocument" should {
    "return a RegistrationDeletion Success" in new Setup {
      mockHttpDelete[RegistrationDeletion.Value](RegistrationDeletion.success)

      val result: RegistrationDeletion.Value = await(connector.deleteRejectedRegistrationDocument("testRegId", "testTxID"))
      result mustBe RegistrationDeletion.success
    }
    "return a Exception on error" in new Setup {
      mockHttpFailedDelete[RegistrationDeletion.Value](UpstreamErrorResponse("msg", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))

      intercept[Exception](await(connector.deleteRejectedRegistrationDocument("testRegId", "testTxID")))
    }
  }

  "deleteRegistrationForRejectedIncorp" should {
    "return a success" in new Setup {
      mockHttpDelete[RegistrationDeletion.Value](RegistrationDeletion.success)

      val result: RegistrationDeletion.Value = await(connector.deleteRegistrationForRejectedIncorp("testRegId", "testTxId"))
      result mustBe RegistrationDeletion.success
    }
    "throw an UpstreamErrorResponse" in new Setup {
      mockHttpFailedDelete[RegistrationDeletion.Value](UpstreamErrorResponse("msg", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))

      intercept[UpstreamErrorResponse](await(connector.deleteRegistrationForRejectedIncorp("testRegId", "testTxId")))
    }
  }
}
