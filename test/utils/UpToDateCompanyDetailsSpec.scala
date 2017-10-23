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

package utils

import fixtures.{CoHoAPIFixture, S4LFixture}
import models.view.{CompanyDetails => CompanyDetailsView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import services.{CompanyDetailsService, IncorporationInformationService, S4LService}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UpToDateCompanyDetailsSpec extends PAYERegSpec with CoHoAPIFixture with S4LFixture {
  val mockIncorpInfoService = mock[IncorporationInformationService]
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockS4LService = mock[S4LService]

  object TestUpToDateCompanyDetails extends UpToDateCompanyDetails {
    override val incorpInfoService = mockIncorpInfoService
    override val companyDetailsService = mockCompanyDetailsService
    override val s4LService = mockS4LService
  }

  implicit val hc = HeaderCarrier()
  def testFunc(details: CompanyDetailsView): Future[Result] = Future.successful(Ok(Json.toJson(details)))

  "calling withLatestCompanyDetails" should {
    "return an up to date Company Details with Incorporation Information data" in {
      val defaultCompanyDetails = CompanyDetailsView("test Name", None, validAddress, None, None)
      val expectedCompanyDetails = CompanyDetailsView(validCoHoCompanyDetailsResponse.companyName, None, validCoHoCompanyDetailsResponse.roAddress, None, None)

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(validCoHoCompanyDetailsResponse))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(defaultCompanyDetails))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      val result = await(TestUpToDateCompanyDetails.withLatestCompanyDetails("testRegId", "testTxId"){testFunc})
      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(expectedCompanyDetails)
    }

    "return a default Company Details from Company Details service if Incorporation Information returns an error" in {
      val defaultCompanyDetails = CompanyDetailsView("test Name", None, validAddress, None, None)

      when(mockIncorpInfoService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Exception))

      when(mockCompanyDetailsService.getCompanyDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(defaultCompanyDetails))

      when(mockS4LService.saveForm[CompanyDetailsView](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("key", Map.empty)))

      val result = await(TestUpToDateCompanyDetails.withLatestCompanyDetails("testRegId", "testTxId"){testFunc})
      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(defaultCompanyDetails)
    }
  }
}
