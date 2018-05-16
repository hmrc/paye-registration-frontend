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

package services

import enums.PAYEStatus
import helpers.PayeComponentSpec
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class CurrentProfileServiceSpec extends PayeComponentSpec with GuiceOneAppPerSuite {

  class Setup {
    val service = new CurrentProfileService {
      override val businessRegistrationConnector  = mockBusinessRegistrationConnector
      override val companyRegistrationConnector   = mockCompRegConnector
      override val payeRegistrationConnector      = mockPAYERegConnector
      override val keystoreConnector              = mockKeystoreConnector
    }
  }

  val validBusinessProfile = Fixtures.validBusinessRegistrationResponse
  val validCompanyProfile = CompanyRegistrationProfile("held", "txId")

  val validCurrentProfile = CurrentProfile(
    validBusinessProfile.registrationID,
    validCompanyProfile,
    validBusinessProfile.language,
    payeRegistrationSubmitted = false,
    None
  )

  "fetchAndStoreCurrentProfile" should {

    "Return a successful outcome after successfully storing a valid Business Registration response" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(validBusinessProfile))

      when(mockCompRegConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      when(mockKeystoreConnector.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map.empty)))

      when(mockPAYERegConnector.getStatus(ArgumentMatchers.contains(validBusinessProfile.registrationID))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(PAYEStatus.draft)))

      await(service.fetchAndStoreCurrentProfile) mustBe validCurrentProfile
    }

    "Return an unsuccessful outcome when there is no record in Business Registration" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("")))

      intercept[NotFoundException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when the user is not authorised for Business Registration" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("")))

      intercept[ForbiddenException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when Business Registration returns an error response" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("")))

      intercept[RuntimeException](await(service.fetchAndStoreCurrentProfile))
    }
  }

  "regSubmitted" should {
    "return false when status is draft" in new Setup {
      service.regSubmitted(Some(PAYEStatus.draft)) mustBe false
    }

    "return false when status is invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.invalid)) mustBe false
    }

    "return true when status is neither draft nor invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.held)) mustBe true
      service.regSubmitted(Some(PAYEStatus.submitted)) mustBe true
      service.regSubmitted(Some(PAYEStatus.acknowledged)) mustBe true
      service.regSubmitted(Some(PAYEStatus.rejected)) mustBe true
      service.regSubmitted(Some(PAYEStatus.cancelled)) mustBe true
    }

    "return false when no status is returned" in new Setup {
      service.regSubmitted(None) mustBe false
    }
  }
}
