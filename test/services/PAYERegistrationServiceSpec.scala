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

package services

import enums.{DownstreamOutcome, IncorporationStatus, RegistrationDeletion}
import helpers.PayeComponentSpec
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class PAYERegistrationServiceSpec extends PayeComponentSpec {

  implicit val request: FakeRequest[_] = FakeRequest()

  class Setup {
    val service: PAYERegistrationService = new PAYERegistrationService(
      payeRegistrationConnector = mockPAYERegConnector,
      keyStoreConnector = mockKeystoreConnector,
      currentProfileService = mockCurrentProfileService,
      s4LService = mockS4LService
    ) {
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  val validCurrentProfile: CurrentProfile = CurrentProfile("testRegId",
    CompanyRegistrationProfile("rejected", "txId"),
    "en",
    payeRegistrationSubmitted = false,
    None)

  val forbidden: UpstreamErrorResponse = UpstreamErrorResponse("403", 403, 403)
  val notFound = new NotFoundException("404")
  val runTimeException = new RuntimeException("tst")

  "Calling assertRegistrationFootprint" should {
    "return a success response when the Registration is successfully created" in new Setup {
      when(mockPAYERegConnector.createNewRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      await(service.assertRegistrationFootprint("123", "txID")) mustBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      when(mockPAYERegConnector.createNewRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      await(service.assertRegistrationFootprint("123", "txID")) mustBe DownstreamOutcome.Failure
    }
  }

  "deleteRejectedRegistration" should {
    "return a RegistrationDeletionSuccess" when {
      "the users paye document was deleted" in new Setup {
        when(mockPAYERegConnector.deleteRejectedRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

        when(mockKeystoreConnector.remove()(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))

        val result: RegistrationDeletion.Value = await(service.deleteRejectedRegistration("testRegId", "testTxId"))
        result mustBe RegistrationDeletion.success
      }
    }

    "return a RegistrationDeletionInvalidStatus" when {
      "the users paye document was not deleted" in new Setup {
        when(mockPAYERegConnector.deleteRejectedRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.invalidStatus))

        val result: RegistrationDeletion.Value = await(service.deleteRejectedRegistration("testRegId", "testTxId"))
        result mustBe RegistrationDeletion.invalidStatus
      }
    }
  }

  "handleIIResponse" should {
    "return a success" when {
      "there is no regId in the current profile" in new Setup {
        when(mockCurrentProfileService.updateCurrentProfileWithIncorpStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(None))

        when(mockPAYERegConnector.getRegistrationId(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future("testRegId"))

        when(mockS4LService.clear(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(HttpResponse(200, "")))

        when(mockPAYERegConnector.deleteRegistrationForRejectedIncorp(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        val result: RegistrationDeletion.Value = await(service.handleIIResponse(txId = "testTxId", status = IncorporationStatus.rejected))
        result mustBe RegistrationDeletion.success
      }

      "there is a regId in the current profile" in new Setup {
        when(mockCurrentProfileService.updateCurrentProfileWithIncorpStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(Some("testRegId")))

        when(mockS4LService.clear(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(HttpResponse(200, "")))

        when(mockPAYERegConnector.deleteRegistrationForRejectedIncorp(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(RegistrationDeletion.success))

        val result: RegistrationDeletion.Value = await(service.handleIIResponse(txId = "testTxId", status = IncorporationStatus.rejected))
        result mustBe RegistrationDeletion.success
      }
    }

    "return an invalidStatus" in new Setup {
      when(mockCurrentProfileService.updateCurrentProfileWithIncorpStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(None))

      val result: RegistrationDeletion.Value = await(service.handleIIResponse(txId = "testTxId", status = IncorporationStatus.accepted))
      result mustBe RegistrationDeletion.invalidStatus
    }
  }
}
