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

import common.exceptions.DownstreamExceptions
import enums.CacheKeys
import fixtures.KeystoreFixture
import models.externalAPIModels.currentProfile.CurrentProfile
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

class CommonServiceSpec extends PAYERegSpec with KeystoreFixture {

  trait Setup {
    val service = new CommonService with DownstreamExceptions {
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  implicit val hc = HeaderCarrier()

  "fetchRegistrationID" should {
    "return a registrationID if one exists in keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))

      await(service.fetchRegistrationID) shouldBe "12345"
    }

    "throw a CurrentProfileNotFound Exception when a registrationID does not exist in keystore" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, None)

      a[DownstreamExceptions.CurrentProfileNotFoundException] shouldBe thrownBy (await(service.fetchRegistrationID))
    }
  }
}
