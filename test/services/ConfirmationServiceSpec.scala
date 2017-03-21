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

import connectors.PAYERegistrationConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ConfirmationServiceSpec extends PAYERegSpec {
  val mockRegConnector = mock[PAYERegistrationConnector]

  class Setup {
    val service = new ConfirmationSrv {
      val payeRegistrationConnector = mockRegConnector
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Calling getAcknowledgementReference" should {
    "return an acknowledgment reference" in new Setup {

      when(mockRegConnector.getAcknowledgementReference(ArgumentMatchers.contains("45632"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("BRPY00000000001")))

      await(service.getAcknowledgementReference("45632")) shouldBe Some("BRPY00000000001")
    }
  }
}
