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
import enums.{DownstreamOutcome, UserCapacity}
import models.view.CompletionCapacity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class CompletionCapacityServiceSpec extends PAYERegSpec {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockKSConnector = mock[KeystoreConnector]
  val mockBusRegConnector = mock[BusinessRegistrationConnector]

  class Setup {
    val service = new CompletionCapacitySrv {
      override val payeRegConnector: PAYERegistrationConnect = mockPAYERegConnector
      override val businessRegistrationConnector: BusinessRegistrationConnect = mockBusRegConnector
    }
  }

  "Calling saveCompletionCapacity" should {
    "return success for a successful save" in new Setup {
      val jobTitle = "Grand Vizier"
      val tstCapacity = CompletionCapacity(UserCapacity.other, "Grand Vizier")

      when(mockPAYERegConnector.upsertCompletionCapacity(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(jobTitle))

      await(service.saveCompletionCapacity(tstCapacity, "12345")) shouldBe DownstreamOutcome.Success
    }
  }

  "Calling viewToAPI" should {
    "return the correct string for a director" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.director, "")) shouldBe "director"
    }
    "return the correct string for an agent" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.agent, "")) shouldBe "agent"
    }
    "return the correct string for an other - Priest" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.other, "Priest")) shouldBe "Priest"
    }
  }

  "Calling apiToView" should {
    "return the correct model for a director" in new Setup {
      service.apiToView("director") shouldBe CompletionCapacity(UserCapacity.director, "")
    }
    "return the correct model for an agent" in new Setup {
      service.apiToView("agent") shouldBe CompletionCapacity(UserCapacity.agent, "")
    }
    "return the correct model for an Agent" in new Setup {
      service.apiToView("Agent") shouldBe CompletionCapacity(UserCapacity.agent, "")
    }
    "return the correct model for an other - Priestess" in new Setup {
      service.apiToView("Priestess") shouldBe CompletionCapacity(UserCapacity.other, "Priestess")
    }
  }

}
