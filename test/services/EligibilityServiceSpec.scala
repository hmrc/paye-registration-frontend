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

import connectors.{PAYERegistrationConnect, PAYERegistrationConnector}
import enums.DownstreamOutcome
import models.api.{Eligibility => EligibilityAPI}
import models.view.{CompanyEligibility, DirectorEligibility, Eligibility => EligibilityView}
import testHelpers.PAYERegSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsString
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class EligibilityServiceSpec extends PAYERegSpec {

  private val mockPayeRegConnector = mock[PAYERegistrationConnector]
  private val mockS4LService = mock[S4LService]

  class Setup {
    val service = new EligibilitySrv {
      override val s4lService: S4LSrv = mockS4LService
      override val payeRegConnector: PAYERegistrationConnect = mockPayeRegConnector
    }
  }

  def apiModel(b1: Boolean, b2: Boolean) = EligibilityAPI(companyEligibility = b1, directorEligibility = b2)

  def viewModel(b1: Option[CompanyEligibility], b2: Option[DirectorEligibility]) = EligibilityView(companyEligible = b1, directorEligible = b2)

  def cE(b: Boolean) = CompanyEligibility(b)
  def dE(b: Boolean) = DirectorEligibility(b)

  "calling apiToView" should {
    "return the corresponding converted Employment API Model for every variation of view" in new Setup {
      service.apiToView(apiModel(true, true)) shouldBe EligibilityView(Some(cE(true)), Some(dE(true)))
      service.apiToView(apiModel(true, false)) shouldBe EligibilityView(Some(cE(true)), Some(dE(false)))
      service.apiToView(apiModel(false, true)) shouldBe EligibilityView(Some(cE(false)), Some(dE(true)))
      service.apiToView(apiModel(false, false)) shouldBe EligibilityView(Some(cE(false)), Some(dE(false)))
    }
  }

  "isCompleteData" should {
    "return the corresponding converted Employment View Model for every variation of stored data" in new Setup {
      service.isCompleteData(viewModel(Some(cE(false)), Some(dE(false)))) shouldBe Right(EligibilityAPI(false, false))
      service.isCompleteData(viewModel(Some(cE(false)), Some(dE(true)))) shouldBe Right(EligibilityAPI(false, true))
      service.isCompleteData(viewModel(Some(cE(true)), Some(dE(false)))) shouldBe Right(EligibilityAPI(true, false))
      service.isCompleteData(viewModel(Some(cE(true)), Some(dE(true)))) shouldBe Right(EligibilityAPI(true, true))

      service.isCompleteData(viewModel(Some(cE(true)), None)) shouldBe Left(viewModel(Some(cE(true)), None))
      service.isCompleteData(viewModel(None, Some(dE(true)))) shouldBe Left(viewModel(None, Some(dE(true))))
      service.isCompleteData(viewModel(None, None)) shouldBe Left(viewModel(None, None))
    }
  }

  "convertOrCreatePAYEContactView" should {
    "convert to view" in new Setup {
      val apiModel = Some(EligibilityAPI(true, true))

      service.convertOrCreateEligibilityView(apiModel) shouldBe EligibilityView(Some(cE(true)), Some(dE(true)))
    }
    "create a new view" in new Setup {
      service.convertOrCreateEligibilityView(None) shouldBe EligibilityView(None, None)
    }
  }

  implicit val hc = new HeaderCarrier()
  val regId = "12345"

  "getEligibility" should {
    val validViewModel = viewModel(Some(cE(true)), Some(dE(true)))
    "should fetch from save4later" in new Setup {

      when(mockS4LService.fetchAndGet[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validViewModel)))

      await(service.getEligibility(regId)) shouldBe validViewModel
    }
    "should fetch from paye registration" in new Setup {
      val validModel = apiModel(true, true)

      when(mockS4LService.fetchAndGet[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockPayeRegConnector.getEligibility(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validModel)), Future.successful(None))

      await(service.getEligibility(regId)) shouldBe validViewModel
      await(service.getEligibility(regId)) shouldBe EligibilityView(None, None)
    }
  }

  "submitEligibility" should {
    "save to save4later" in new Setup {
      val data = viewModel(Some(cE(true)), None)

      when(mockS4LService.saveForm[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.any[EligibilityView], ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(CacheMap("", Map("" -> JsString(""))))

      await(service.submitEligibility(regId, data)) shouldBe DownstreamOutcome.Success
    }
    "save to paye registration" in new Setup {
      val data = viewModel(Some(cE(true)), Some(dE(true)))

      when(mockPayeRegConnector.upsertEligibility(ArgumentMatchers.eq(regId), ArgumentMatchers.any[EligibilityAPI])(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(apiModel(true, true)))

      when(mockS4LService.clear(ArgumentMatchers.eq(regId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      await(service.submitEligibility(regId, data)) shouldBe DownstreamOutcome.Success
    }
  }

  "saveCompany" should {
    "be able to save both a complete model or an incomplete model" in new Setup {
      when(mockS4LService.fetchAndGet[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(viewModel(None, None))))
      when(mockS4LService.saveForm[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.any[EligibilityView], ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(CacheMap("", Map("" -> JsString(""))))

      await(service.saveCompanyEligibility(regId, CompanyEligibility(true))) shouldBe DownstreamOutcome.Success
    }
  }

  "saveDirector" should {
    "be able to save both a complete model or an incomplete model" in new Setup {
      when(mockS4LService.fetchAndGet[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(viewModel(None, None))))
      when(mockS4LService.saveForm[EligibilityView](ArgumentMatchers.anyString(), ArgumentMatchers.any[EligibilityView], ArgumentMatchers.eq(regId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(CacheMap("", Map("" -> JsString(""))))

      await(service.saveDirectorEligibility(regId, DirectorEligibility(true))) shouldBe DownstreamOutcome.Success
    }
  }

}
