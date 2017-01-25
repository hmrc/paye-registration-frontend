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
import play.mvc.Http.Status
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http._

class TestPAYERegConnectorSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockPAYERegConnector = mock[PAYERegistrationConnector]

  class Setup {
    val connector = new TestPAYERegConnector {
      override val payeRegUrl: String = "tst-url"
      override val http: HttpGet with HttpPost with HttpPatch = mockWSHttp
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector
    }
  }

  implicit val hc = HeaderCarrier()

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
