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

import connectors.{PAYERegistrationConnect, BusinessRegistrationConnect, CompanyRegistrationConnect}
import enums.{PAYEStatus, CacheKeys}
import fixtures.BusinessRegistrationFixture
import models.external.{CompanyRegistrationProfile, BusinessProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class CurrentProfileServiceSpec extends PAYERegSpec with BusinessRegistrationFixture {

  val mockCompanyRegistrationConnector = mock[CompanyRegistrationConnect]
  val mockPAYERegistrationConnector = mock[PAYERegistrationConnect]

  class Setup {
    val service = new CurrentProfileSrv {
      override val businessRegistrationConnector: BusinessRegistrationConnect = mockBusinessRegistrationConnector
      override val companyRegistrationConnector: CompanyRegistrationConnect = mockCompanyRegistrationConnector
      override val payeRegistrationConnector: PAYERegistrationConnect = mockPAYERegistrationConnector
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  implicit val hc = HeaderCarrier()
  val validBusinessProfile = validBusinessRegistrationResponse
  val validCompanyProfile = CompanyRegistrationProfile("held", "txId")

  val validCurrentProfile = CurrentProfile(
                              validBusinessProfile.registrationID,
                              validBusinessProfile.completionCapacity,
                              validCompanyProfile,
                              validBusinessProfile.language,
                              payeRegistrationSubmitted = false
                            )

  "fetchAndStoreCurrentProfile" should {

    "Return a successful outcome after successfully storing a valid Business Registration response" in new Setup {
      mockBusinessRegFetch(Future.successful(validBusinessProfile))

      when(mockCompanyRegistrationConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      mockKeystoreCache[BusinessProfile](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      when(mockPAYERegistrationConnector.getStatus(ArgumentMatchers.contains(validBusinessProfile.registrationID))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(PAYEStatus.draft)))

      await(service.fetchAndStoreCurrentProfile) shouldBe validCurrentProfile
    }

    "Return an unsuccessful outcome when there is no record in Business Registration" in new Setup {
      mockBusinessRegFetch(Future.failed(new NotFoundException("")))

      intercept[NotFoundException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when the user is not authorised for Business Registration" in new Setup {
      mockBusinessRegFetch(Future.failed(new ForbiddenException("")))

      intercept[ForbiddenException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when Business Registration returns an error response" in new Setup {
      mockBusinessRegFetch(Future.failed(new RuntimeException("")))

      intercept[RuntimeException](await(service.fetchAndStoreCurrentProfile))
    }
  }

  "regSubmitted" should {
    "return false when status is draft" in new Setup {
      service.regSubmitted(Some(PAYEStatus.draft)) shouldBe false
    }
    "return false when status is invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.invalid)) shouldBe false
    }
    "return true when status is neither draft nor invalid" in new Setup {
      service.regSubmitted(Some(PAYEStatus.held)) shouldBe true
      service.regSubmitted(Some(PAYEStatus.submitted)) shouldBe true
      service.regSubmitted(Some(PAYEStatus.acknowledged)) shouldBe true
      service.regSubmitted(Some(PAYEStatus.rejected)) shouldBe true
      service.regSubmitted(Some(PAYEStatus.cancelled)) shouldBe true
    }
    "return false when no status is returned" in new Setup {
      service.regSubmitted(None) shouldBe false
    }
  }

}
