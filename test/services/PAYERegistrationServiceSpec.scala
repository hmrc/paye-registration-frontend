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

package services

import connectors._
import enums.DownstreamOutcome
import fixtures.{KeystoreFixture, PAYERegistrationFixture}
import common.exceptions.DownstreamExceptions.PAYEMicroserviceException
import models.payeRegistration.PAYERegistration
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PAYERegSpec with PAYERegistrationFixture with KeystoreFixture {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new PAYERegistrationService {
      override val payeRegistrationConnector = mockRegConnector
      override val s4LService = mockS4LService
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  implicit val hc = HeaderCarrier()

  "Calling fetchAndStoreCurrentRegistration" should {

    "throw the correct exception when the microservice sends back a Bad Request response" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.getCurrentRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationBadRequestResponse))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.fetchAndStoreCurrentRegistration()))
    }

    "throw the correct exception when the microservice sends back a Forbidden response" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.getCurrentRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationForbiddenResponse))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.fetchAndStoreCurrentRegistration()))
    }

    "throw the correct exception when the microservice sends back an Internal error response" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.getCurrentRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst"))))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.fetchAndStoreCurrentRegistration()))
    }

    "return an empty option when there is no Registration returned by the microservice" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.getCurrentRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationNotFoundResponse))

      await(service.fetchAndStoreCurrentRegistration()) shouldBe None
    }

    "return the saved Registration object when it is retrieved from the microservice" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.getCurrentRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validPAYERegistration)))
      when(mockS4LService.saveRegistration(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(validPAYERegistration))

      await(service.fetchAndStoreCurrentRegistration()) shouldBe Some(validPAYERegistration)
    }
  }

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validPAYERegistration)))

      await(service.createNewRegistration()) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationBadRequestResponse))

      await(service.createNewRegistration()) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling addTestRegistration" should {
    "return a success response when correctly adding a registration with CompanyDetails" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.addTestRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validPAYERegistration)))

      await(service.addTestRegistration(validCompanyDetails)) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when unable to add a registration with CompanyDetails" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.addTestRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst msg"))))

      await(service.addTestRegistration(validCompanyDetails)) shouldBe DownstreamOutcome.Failure
    }

    "return a success response when correctly adding a registration with a full PAYERegistration" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.addTestRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validPAYERegistration)))

      await(service.addTestRegistration(validPAYERegistration)) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when unable to add a registration with a full PAYERegistration" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.addTestRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst msg"))))

      await(service.addTestRegistration(validPAYERegistration)) shouldBe DownstreamOutcome.Failure
    }
  }
}
