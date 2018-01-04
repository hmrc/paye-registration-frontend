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

import builders.AuthBuilder
import connectors._
import fixtures.{CoHoAPIFixture, KeystoreFixture}
import models.external.{Ticket => ApiTTicket}
import models.view.{Ticket => TicketForm}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class DeskproServiceSpec extends PAYERegSpec with KeystoreFixture with CoHoAPIFixture with AuthBuilder {

  val mockdeskproConnector = mock[DeskproConnector]

  trait Setup {
    val service = new DeskproSrv {
      override val authConnector: AuthConnector = mockAuthConnector
      override val deskproConnector: DeskproConnect = mockdeskproConnector
    }
  }

  val regId = "12345"
  val mockAuthId = "auth/oid/1234567890"

  val sessionId = "session-123456-654321"
  val mockSession = SessionId(sessionId)
  implicit val hc = HeaderCarrier(sessionId = Some(mockSession))

  "getAuthId" should {
    "return a successful AuthId" in new Setup {
      mockAuthorisedUser(mockAuthId, mockAuthConnector)

      await(service.getAuthId) shouldBe mockAuthId
    }
  }

  "getSessionId" should {
    "return a session Id" in new Setup {
      service.getSessionId shouldBe sessionId
    }
  }


  val name = "Mr Bobby B. Bobblington III MBE BSc"
  val email = "thebigb@testmail.test"
  val message = "testMessage"

  val ticket: ApiTTicket = ApiTTicket(
    name = name,
    email = email,
    subject = s"PAYE Registration submission failed for Registration ID: $regId",
    message = message,
    referrer = "https://www.tax.service.gov.uk/register-for-paye",
    javascriptEnabled = "Y",
    userAgent = "paye-registration-frontend",
    authId = mockAuthId,
    areaOfTax = "unknown",
    sessionId = sessionId,
    service = "SCRS"
  )

  val providedInfo = TicketForm(name, email, message)

  "buildTicket" should {
    "return a new ticket" in new Setup {
      mockAuthorisedUser(mockAuthId, mockAuthConnector)

      await(service.buildTicket(regId, providedInfo)) shouldBe ticket
    }
  }

  "submitTicket" should {

    val ticketResponse : Long = 123456789

    "return a ticket id fromt DeskPro" in new Setup {
      mockAuthorisedUser(mockAuthId, mockAuthConnector)

      when(mockdeskproConnector.submitTicket(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(ticketResponse))

      await(service.submitTicket(regId, providedInfo)) shouldBe ticketResponse
    }
  }

}
