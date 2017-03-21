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

import java.time.LocalDate

import connectors.{PAYERegistrationConnect, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome}
import fixtures.{PAYERegistrationFixture, S4LFixture}
import models.api.{Employment => EmploymentAPI}
import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify, when}
import play.api.libs.json.{Format, Json}
import testHelpers.PAYERegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.DateUtil

import scala.concurrent.Future

class EmploymentServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {
  val now = LocalDate.now()
  implicit val hc = HeaderCarrier()
  implicit val formatEmploymentView = Json.format[EmploymentView]

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  val returnCacheMap = CacheMap("", Map("" -> Json.toJson("")))
  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new EmploymentSrv {
      override val payeRegConnector = mockPAYERegConnector
      override val s4LService = mockS4LService
    }

    val dateUtil = new DateUtil {}
  }

  "calling viewToAPI with EmployingStaff" should {
    "return the corresponding converted Employment API Model with CompanyPension" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) shouldBe Right(EmploymentAPI(true, Some(true), true, now))
    }

    "return the corresponding converted Employment API Model without CompanyPension" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) shouldBe Right(EmploymentAPI(false, None, true, now))
    }

    "return the Employment VIEW Model with EmployingStaff set as true and CompanyPension set as None" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(true)), None, Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }

    "return the Employment VIEW Model with Subcontractors set as None" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }

    "return the Employment VIEW Model with FirstPayment set as None" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, None)
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling apiToView with EmployingStaff" should {
    "return the corresponding converted Employment View Model with CompanyPension" in new Setup {
      val apiModel = EmploymentAPI(true, Some(true), false, now)
      val expected = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(false)), Some(FirstPaymentView(LocalDate.now())))
      service.apiToView(apiModel) shouldBe expected
    }

    "return the corresponding converted Employment View Model without CompanyPension" in new Setup {
      val apiModel = EmploymentAPI(false, None, false, now)
      service.apiToView(apiModel) shouldBe EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(false)), Some(FirstPaymentView(LocalDate.now())))
    }
  }

  "calling fetchEmploymentView" should {
    "return the Employment VIEW model if found in S4L" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(validEmploymentViewModel)))

      await(service.fetchEmploymentView("54321")) shouldBe validEmploymentViewModel
    }

    "return the Employment VIEW model from the connector if not found in S4L" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getEmployment(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validEmploymentAPIModel)))

      await(service.fetchEmploymentView("54321")) shouldBe service.apiToView(validEmploymentAPIModel)
    }

    "return an empty Employment VIEW model if not found in S4L or in connector" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getEmployment(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      await(service.fetchEmploymentView("54321")) shouldBe EmploymentView(None, None, None, None)
    }
  }

  "calling saveEmploymentView" should {
    "save the Employment VIEW model in S4L if the model is incomplete" in new Setup {
      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveEmploymentView(incompleteEmploymentViewModel, "54321")) shouldBe S4LSaved
    }

    "save the Employment VIEW model in BE if the model is complete" in new Setup {
      when(mockPAYERegConnector.upsertEmployment(ArgumentMatchers.eq("54321"), ArgumentMatchers.eq(validEmploymentAPIModel))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmploymentAPIModel))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveEmploymentView(validEmploymentViewModel, "54321")) shouldBe MongoSaved
    }

    "clear S4L data if the Employment VIEW model is saved in BE" in new Setup {
      when(mockPAYERegConnector.upsertEmployment(ArgumentMatchers.eq("54321"), ArgumentMatchers.eq(validEmploymentAPIModel))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validEmploymentAPIModel))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveEmployment(validEmploymentViewModel, "54321"))
      verify(mockS4LService, times(1)).clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]())
    }
  }

  "calling saveEmployingStaff" should {
    "update the Employment VIEW model" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveEmployingStaff(EmployingStaff(false), "54321")) shouldBe DownstreamOutcome.Success
    }
  }

  "calling saveCompanyPension" should {
    "update the Employment VIEW model" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveCompanyPension(CompanyPension(false), "54321")) shouldBe DownstreamOutcome.Success
    }
  }

  "calling saveSubcontractors" should {
    "update the Employment VIEW model" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveSubcontractors(Subcontractors(false), "54321")) shouldBe DownstreamOutcome.Success
    }
  }

  "calling saveFirstPayment" should {
    "update the Employment VIEW model" in new Setup {
      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveFirstPayment(FirstPaymentView(LocalDate.of(2016, 12, 1)), "54321")) shouldBe DownstreamOutcome.Success
    }
  }
}
