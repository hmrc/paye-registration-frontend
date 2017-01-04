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

import connectors.{CohoApiErrorResponse, CohoApiBadRequestResponse, CohoApiSuccessResponse, CoHoAPIConnector}
import enums.{DownstreamOutcome, CacheKeys}
import fixtures.{CoHoAPIFixture, KeystoreFixture}
import helpers.PAYERegSpec
import models.coHo.CoHoCompanyDetailsModel
import models.currentProfile.CurrentProfile
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CoHoAPIServiceSpec extends PAYERegSpec with KeystoreFixture with CoHoAPIFixture {

  val mockCoHoAPIConnector = mock[CoHoAPIConnector]

  trait Setup {
    val service = new CoHoAPIService {
      override val coHoAPIConnector = mockCoHoAPIConnector
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  val tstSuccessResult = CohoApiSuccessResponse(validCoHoCompanyDetailsResponse)
  val tstBadRequestResult = CohoApiBadRequestResponse
  val tstInternalErrorResult = CohoApiErrorResponse(new RuntimeException)

  implicit val hc = HeaderCarrier()

  "fetchAndStoreCompanyDetails" should {

    "return a successful DownstreamOutcome for a successful response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(Matchers.anyString())(Matchers.any())).thenReturn(Future.successful(tstSuccessResult))
      mockKeystoreCache[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, CacheMap("map", Map.empty))

      await(service.fetchAndStoreCoHoCompanyDetails) shouldBe DownstreamOutcome.Success
    }

    "return a failed DownstreamOutcome for a Bad Request response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(Matchers.anyString())(Matchers.any())).thenReturn(Future.successful(tstBadRequestResult))

      await(service.fetchAndStoreCoHoCompanyDetails) shouldBe DownstreamOutcome.Failure
    }

    "return a failed DownstreamOutcome for an Internal Exception response from the CoHo API" in new Setup {
      mockKeystoreFetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString, Some(validCurrentProfileResponse))
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(Matchers.anyString())(Matchers.any())).thenReturn(Future.successful(tstInternalErrorResult))

      await(service.fetchAndStoreCoHoCompanyDetails) shouldBe DownstreamOutcome.Failure
    }
  }

}
