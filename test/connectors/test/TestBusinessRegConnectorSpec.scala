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

package connectors.test

import fixtures.BusinessRegistrationFixture
import models.external.BusinessProfile
import play.api.libs.json.JsValue
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.HeaderCarrier

class TestBusinessRegConnectorSpec extends PAYERegSpec with BusinessRegistrationFixture {

  trait Setup {
    val connector = new TestBusinessRegConnector {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http = mockWSHttp
    }
  }

  implicit val hc = HeaderCarrier()

  "createMetadataEntry" should {
    "make a http POST request to business registration micro-service to create a CurrentProfile entry" in new Setup {
      mockHttpPOST[JsValue, BusinessProfile](connector.businessRegUrl, validBusinessRegistrationResponse)

      await(connector.createBusinessProfileEntry) shouldBe validBusinessRegistrationResponse
    }
  }
}
