/*
 * Copyright 2019 HM Revenue & Customs
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

import enums.{DownstreamOutcome, UserCapacity}
import helpers.PayeComponentSpec
import models.view.CompletionCapacity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._

import scala.concurrent.Future


class CompletionCapacityServiceSpec extends PayeComponentSpec {

  class Setup {
    val service = new CompletionCapacityService {
      override val payeRegConnector              = mockPAYERegConnector
      override val businessRegistrationConnector = mockBusinessRegistrationConnector
    }
  }

  "Calling saveCompletionCapacity" should {
    "return success for a successful save" in new Setup {
      val jobTitle = "Grand Vizier"
      val tstCapacity = CompletionCapacity(UserCapacity.other, "Grand Vizier")

      when(mockPAYERegConnector.upsertCompletionCapacity(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(jobTitle))

      await(service.saveCompletionCapacity("12345", tstCapacity)) mustBe DownstreamOutcome.Success
    }
  }
  "Calling viewToAPI" should {
    "return the correct string for a director" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.director, "")) mustBe "director"
    }
    "return the correct string for an agent" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.agent, "")) mustBe "agent"
    }
    "return the correct string for a secretary" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.secretary, "")) mustBe "company secretary"
    }
    "return the correct string for an other - Priest" in new Setup {
      service.viewToAPI(CompletionCapacity(UserCapacity.other, "Priest")) mustBe "Priest"
    }
  }
  "Calling apiToView" should {
    "return the correct model for a director" in new Setup {
      service.apiToView("director") mustBe CompletionCapacity(UserCapacity.director, "")
    }
    "return the correct model for a secretary" in new Setup {
      service.apiToView("company secretary") mustBe CompletionCapacity(UserCapacity.secretary, "")
    }
    "return the correct model for an agent" in new Setup {
      service.apiToView("agent") mustBe CompletionCapacity(UserCapacity.agent, "")
    }
    "return the correct model for an Agent" in new Setup {
      service.apiToView("Agent") mustBe CompletionCapacity(UserCapacity.agent, "")
    }
    "return the correct model for an other - Priestess" in new Setup {
      service.apiToView("Priestess") mustBe CompletionCapacity(UserCapacity.other, "Priestess")
    }
  }
}
