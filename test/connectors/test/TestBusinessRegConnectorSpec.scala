/*
 * Copyright 2023 HM Revenue & Customs
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

import helpers.PayeComponentSpec
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestBusinessRegConnectorSpec extends PayeComponentSpec {

  class Setup extends CodeMocks {
    val testConnector = new TestBusinessRegConnector {
      override val businessRegUrl = "testBusinessRegUrl"
      override val http = mockHttpClient
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }

  "createMetadataEntry" should {
    "make a http POST request to business registration micro-service to create a CurrentProfile entry" in new Setup {
      mockHttpPOST[JsValue, BusinessProfile](testConnector.businessRegUrl, Fixtures.validBusinessRegistrationResponse)

      when(mockHttpClient.POST[JsValue, BusinessProfile](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validBusinessRegistrationResponse))

      await(testConnector.createBusinessProfileEntry) mustBe Fixtures.validBusinessRegistrationResponse
    }
  }
}
