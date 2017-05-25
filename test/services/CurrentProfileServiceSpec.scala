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

import connectors.{BusinessRegistrationConnect, CompanyRegistrationConnect}
import enums.CacheKeys
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

  class Setup {
    val service = new CurrentProfileSrv {
      override val businessRegistrationConnector: BusinessRegistrationConnect = mockBusinessRegistrationConnector
      override val companyRegistrationConnector: CompanyRegistrationConnect = mockCompanyRegistrationConnector
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
                              validBusinessProfile.language
                            )

  "fetchAndStoreCurrentProfile" should {

    "Return a successful outcome after successfully storing a valid Business Registration response" in new Setup {
      mockBusinessRegFetch(Future.successful(validBusinessProfile))

      when(mockCompanyRegistrationConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      mockKeystoreCache[BusinessProfile](CacheKeys.CurrentProfile.toString, CacheMap("", Map.empty))

      await(service.fetchAndStoreCurrentProfile) shouldBe validCurrentProfile
    }

    "Return an unsuccessful outcome when there is no record in Business Registration" in new Setup {
      mockBusinessRegFetch(Future.failed(new NotFoundException("")))

      when(mockCompanyRegistrationConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      intercept[NotFoundException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when the user is not authorised for Business Registration" in new Setup {
      mockBusinessRegFetch(Future.failed(new ForbiddenException("")))

      when(mockCompanyRegistrationConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      intercept[ForbiddenException](await(service.fetchAndStoreCurrentProfile))
    }

    "Return an unsuccessful outcome when Business Registration returns an error response" in new Setup {
      mockBusinessRegFetch(Future.failed(new RuntimeException("")))

      when(mockCompanyRegistrationConnector.getCompanyRegistrationDetails(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCompanyProfile))

      intercept[RuntimeException](await(service.fetchAndStoreCurrentProfile))
    }
  }

}
