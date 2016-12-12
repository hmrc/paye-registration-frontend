/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.{BusinessRegistrationErrorResponse, BusinessRegistrationForbiddenResponse, BusinessRegistrationNotFoundResponse, BusinessRegistrationSuccessResponse}
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.BusinessRegistrationFixture
import helpers.PAYERegSpec
import models.currentProfile.CurrentProfile
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

class CurrentProfileServiceSpec extends PAYERegSpec with BusinessRegistrationFixture {


  trait Setup {
    val service = new CurrentProfileService {
      override val keystoreConnector = mockKeystoreConnector
      override val businessRegistrationConnector = mockBusinessRegistrationConnector
    }
  }

  implicit val hc = HeaderCarrier()

  "fetchAndStoreCurrentProfile" should {

    "Return a successful outcome after successfully storing a valid Business Registration response" in new Setup {
      val validResponse = BusinessRegistrationSuccessResponse(validBusinessRegistrationResponse)
      mockBusinessRegFetch(validResponse)
      mockKeystoreCache[CurrentProfile](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      await(service.fetchAndStoreCurrentProfile) shouldBe DownstreamOutcome.Success
    }

    "Return an unsuccessful outcome when there is no record in Business Registration" in new Setup {
      mockBusinessRegFetch(BusinessRegistrationNotFoundResponse)

      await(service.fetchAndStoreCurrentProfile) shouldBe DownstreamOutcome.Failure
    }

    "Return an unsuccessful outcome when the user is not authorised for Business Registration" in new Setup {
      mockBusinessRegFetch(BusinessRegistrationForbiddenResponse)

      await(service.fetchAndStoreCurrentProfile) shouldBe DownstreamOutcome.Failure
    }

    "Return an unsuccessful outcome when Business Registration returns an error response" in new Setup {
      mockBusinessRegFetch(BusinessRegistrationErrorResponse(new RuntimeException))

      await(service.fetchAndStoreCurrentProfile) shouldBe DownstreamOutcome.Failure
    }

    "Return an unsuccessful outcome when Keystore returns an error response" in new Setup {
      val validResponse = BusinessRegistrationSuccessResponse(validBusinessRegistrationResponse)
      mockBusinessRegFetch(validResponse)
      mockKeystoreCacheError[CurrentProfile](CacheKeys.CurrentProfile.toString, new RuntimeException)

      await(service.fetchAndStoreCurrentProfile) shouldBe DownstreamOutcome.Failure
    }
  }

}
