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
import fixtures.PAYERegistrationFixture
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PAYERegSpec with PAYERegistrationFixture {

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

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validPAYERegistrationAPI)))

      await(service.createNewRegistration()) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationBadRequestResponse))

      await(service.createNewRegistration()) shouldBe DownstreamOutcome.Failure
    }
  }

}
