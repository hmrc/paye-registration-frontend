/*
 * Copyright 2018 HM Revenue & Customs
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

import enums.{DownstreamOutcome, RegistrationDeletion}
import helpers.PayeComponentSpec
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse}

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PayeComponentSpec {

  class Setup {
    val service = new PAYERegistrationService {
      override val payeRegistrationConnector  = mockPAYERegConnector
      override val keyStoreConnector          = mockKeystoreConnector
      override val currentProfileService      = mockCurrentProfileService
      override val s4LService                 = mockS4LService
    }
  }

  implicit val context = AuthHelpers.buildAuthContext

  val validCurrentProfile = CurrentProfile("testRegId", CompanyRegistrationProfile("rejected", "txId"), "en", false, None)

  val forbidden = Upstream4xxResponse("403", 403, 403)
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

  "deletePayeRegistrationDocument" should {
    "return a RegistrationDeletionSuccess" when {
      "the users paye document was deleted" in new Setup {
        when(mockPAYERegConnector.deleteCurrentRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

        when(mockKeystoreConnector.remove()(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(true))

        val result = await(service.deletePayeRegistrationDocument("testRegId", "testTxId"))
        result mustBe RegistrationDeletion.success
      }
    }

    "return a RegistrationDeletionInvalidStatus" when {
      "the users paye document was not deleted" in new Setup {
        when(mockPAYERegConnector.deleteCurrentRegistrationDocument(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.invalidStatus))

        val result = await(service.deletePayeRegistrationDocument("testRegId", "testTxId"))
        result mustBe RegistrationDeletion.invalidStatus
      }
    }
  }
}
