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

package connectors.test

import connectors.PAYERegistrationConnector
import enums.DownstreamOutcome
import fixtures.PAYERegistrationFixture
import models.api.PAYERegistration
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.mvc.Http.Status
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

class TestPAYERegConnectorSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockPAYERegConnector = mock[PAYERegistrationConnector]

  class Setup {
    val connector = new TestPAYERegConnector(mockKeystoreConnector, mockPAYERegConnector) {
      override val http: WSHttp = mockWSHttp
      override val payeRegUrl: String = "tst-url"
    }
  }

  implicit val hc = HeaderCarrier()

  "Calling addPAYERegistration" should {
    "return a successful outcome for a successful add of PAYE Registration" in new Setup {
      mockHttpPOST[PAYERegistration, HttpResponse]("tst-url", HttpResponse(Status.OK))

      await(connector.addPAYERegistration(validPAYERegistrationAPI)) shouldBe DownstreamOutcome.Success
    }
    "return a failed outcome for an unsuccessful add of PAYE Registration" in new Setup {
      mockHttpPOST[PAYERegistration, HttpResponse]("tst-url", HttpResponse(Status.BAD_REQUEST))

      await(connector.addPAYERegistration(validPAYERegistrationAPI)) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling addTestCompanyDetails" should {
    "return a successful outcome for a successful add of Company Details" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.upsertCompanyDetails(Matchers.contains("54321"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(validCompanyDetailsAPI))

      await(connector.addTestCompanyDetails(validCompanyDetailsAPI)) shouldBe DownstreamOutcome.Success
    }
    "return a failed outcome for an unsuccessful add of Company Details" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.upsertCompanyDetails(Matchers.contains("54321"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new RuntimeException("tst")))

      await(connector.addTestCompanyDetails(validCompanyDetailsAPI)) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling testRegistrationTeardown" should {
    "return a successful outcome for a successful teardown" in new Setup {
      mockHttpGet[HttpResponse]("tst-url", HttpResponse(Status.OK))

      await(connector.testRegistrationTeardown()) shouldBe DownstreamOutcome.Success
    }
    "return a failed outcome for an unsuccessful teardown" in new Setup {
      val e = new RuntimeException("tst")
      mockHttpFailedGET[HttpResponse]("tst-url", e)

      await(connector.testRegistrationTeardown()) shouldBe DownstreamOutcome.Failure
    }
  }
}
