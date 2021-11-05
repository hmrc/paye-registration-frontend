/*
 * Copyright 2021 HM Revenue & Customs
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

import helpers.PayeComponentSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmationServiceSpec extends PayeComponentSpec {

  trait Setup {
    val testNow: LocalDate

    val service = new ConfirmationService {
      def now = testNow

      val startDate = LocalDate.of(2018, 2, 6)
      val endDate = LocalDate.of(2018, 5, 17)
      val payeRegistrationConnector = mockPAYERegConnector
    }
  }

  "Calling getAcknowledgementReference" should {
    "return an acknowledgment reference" in new Setup {
      override val testNow = LocalDate.now

      when(mockPAYERegConnector.getAcknowledgementReference(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("BRPY00000000001")))

      await(service.getAcknowledgementReference("45632")) mustBe Some("BRPY00000000001")
    }
  }

  "determineIfInclusiveContentIsShown" should {
    "return true" when {
      "now is equal to the start date" in new Setup {
        override val testNow = LocalDate.of(2018, 2, 6)

        service.determineIfInclusiveContentIsShown mustBe true
      }

      "now is equal to the end date" in new Setup {
        override val testNow = LocalDate.of(2018, 5, 17)

        service.determineIfInclusiveContentIsShown mustBe true
      }

      "now is after the start date but before the end date" in new Setup {
        override val testNow = LocalDate.of(2018, 4, 4)

        service.determineIfInclusiveContentIsShown mustBe true
      }
    }

    "return false" when {
      "now is before the start date" in new Setup {
        override val testNow = LocalDate.of(2018, 1, 1)

        service.determineIfInclusiveContentIsShown mustBe false
      }

      "now is after the end date" in new Setup {
        override val testNow = LocalDate.of(2018, 10, 26)

        service.determineIfInclusiveContentIsShown mustBe false
      }
    }
  }
}
