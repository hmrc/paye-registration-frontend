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

import common.exceptions.DownstreamExceptions.CompanyDetailsNotFoundException
import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.{CoHoAPIFixture, KeystoreFixture}
import models.external.{BusinessProfile, CoHoCompanyDetailsModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CoHoAPIServiceSpec extends PAYERegSpec with KeystoreFixture with CoHoAPIFixture {

  val mockCoHoAPIConnector = mock[IncorporationInformationConnector]

  trait Setup {
    val service = new IncorporationInformationSrv {
      override val incorpInfoConnector: IncorporationInformationConnect = mockCoHoAPIConnector
      override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
    }
  }

  val tstSuccessResult = IncorpInfoSuccessResponse(validCoHoCompanyDetailsResponse)
  val tstBadRequestResult = IncorpInfoBadRequestResponse
  val tstInternalErrorResult = IncorpInfoErrorResponse(new RuntimeException)

  implicit val hc = HeaderCarrier()

  "fetchAndStoreCompanyDetails" should {

    "return a successful DownstreamOutcome for a successful response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[BusinessProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(tstSuccessResult))
      mockKeystoreCache[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, CacheMap("map", Map.empty))

      await(service.fetchAndStoreCoHoCompanyDetails("regId", "txId")) shouldBe DownstreamOutcome.Success
    }

    "return a failed DownstreamOutcome for a Bad Request response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[BusinessProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(tstBadRequestResult))

      await(service.fetchAndStoreCoHoCompanyDetails("regId", "txId")) shouldBe DownstreamOutcome.Failure
    }

    "return a failed DownstreamOutcome for an Internal Exception response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[BusinessProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(tstInternalErrorResult))

      await(service.fetchAndStoreCoHoCompanyDetails("regId", "txId")) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getCompanyName" should {
    "return the Company Name when there is a CoHo company details model stored in keystore" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, Some(validCoHoCompanyDetailsResponse))

      await(service.getStoredCompanyDetails()) shouldBe validCoHoCompanyDetailsResponse
    }

    "throw a CompanyDetailsNotFoundException when there is no CoHo company details model stored in keystore" in new Setup {
      mockKeystoreFetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, None)

      a[CompanyDetailsNotFoundException] shouldBe thrownBy(await(service.getStoredCompanyDetails()))
    }
  }

  "Calling getDirectorDetails" should {
    "return the directors details when there is Officer list in CoHo API" in new Setup {
      when(mockCoHoAPIConnector.getOfficerList(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validOfficerList))

      await(service.getDirectorDetails("testTransactionId")) shouldBe validDirectorDetails
    }
  }

}
