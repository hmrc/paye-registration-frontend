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

import java.time.LocalDate

import enums.CacheKeys
import helpers.PayeComponentSpec
import models.api.{Employing, EmploymentV2, Employment => EmploymentAPI}
import models.view.{CompanyPension, EmployingAnyone, EmployingStaff, EmployingStaffV2, Subcontractors, WillBePaying, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify, when}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.DateUtil

import scala.concurrent.Future

class EmploymentServiceSpec extends PayeComponentSpec {
  val now = LocalDate.now()
  implicit val formatEmploymentView = Json.format[EmploymentView]

  val returnCacheMap = CacheMap("", Map("" -> Json.toJson("")))
  val returnHttpResponse = HttpResponse(200)

  trait Setup {
    def testNow: LocalDate

    val service = new EmploymentService {
      override def now              = testNow
      override val payeRegConnector = mockPAYERegConnector
      override val s4LService       = mockS4LService
    }

    val dateUtil = new DateUtil {}
  }
  "calling viewToAPI with EmployingStaff" should {
    "return the corresponding converted Employment API Model with CompanyPension" in new Setup {
      override def testNow = LocalDate.now

      val viewModel = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) mustBe Right(EmploymentAPI(true, Some(true), true, now))
    }

    "return the corresponding converted Employment API Model without CompanyPension" in new Setup {
      override def testNow = LocalDate.now

      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) mustBe Right(EmploymentAPI(false, None, true, now))
    }

    "return the Employment VIEW Model with EmployingStaff set as true and CompanyPension set as None" in new Setup {
      override def testNow = LocalDate.now

      val viewModel = EmploymentView(Some(EmployingStaff(true)), None, Some(Subcontractors(true)), Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) mustBe Left(viewModel)
    }

    "return the Employment VIEW Model with Subcontractors set as None" in new Setup {
      override def testNow = LocalDate.now

      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, Some(FirstPaymentView(LocalDate.now())))
      service.viewToAPI(viewModel) mustBe Left(viewModel)
    }

    "return the Employment VIEW Model with FirstPayment set as None" in new Setup {
      override def testNow = LocalDate.now

      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, None)
      service.viewToAPI(viewModel) mustBe Left(viewModel)
    }
  }

  "calling apiToView with EmployingStaff" should {
    "return the corresponding converted Employment View Model with CompanyPension" in new Setup {
      override def testNow = LocalDate.now

      val apiModel = EmploymentAPI(true, Some(true), false, now)
      val expected = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(false)), Some(FirstPaymentView(LocalDate.now())))
      service.apiToView(apiModel) mustBe expected
    }

    "return the corresponding converted Employment View Model without CompanyPension" in new Setup {
      override def testNow = LocalDate.now

      val apiModel = EmploymentAPI(false, None, false, now)
      service.apiToView(apiModel) mustBe EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(false)), Some(FirstPaymentView(LocalDate.now())))
    }
  }

  "calling fetchEmploymentView" should {
    "return the Employment VIEW model if found in S4L" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(Fixtures.validEmploymentViewModel)))

      await(service.fetchEmploymentView("54321")) mustBe Fixtures.validEmploymentViewModel
    }

    "return the Employment VIEW model from the connector if not found in S4L" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getEmployment(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(Fixtures.validEmploymentAPIModel)))

      await(service.fetchEmploymentView("54321")) mustBe service.apiToView(Fixtures.validEmploymentAPIModel)
    }

    "return an empty Employment VIEW model if not found in S4L or in connector" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getEmployment(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      await(service.fetchEmploymentView("54321")) mustBe EmploymentView(None, None, None, None)
    }
  }

  "calling saveEmploymentView" should {
    "save the Employment VIEW model in S4L if the model is incomplete" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveEmploymentView(Fixtures.incompleteEmploymentViewModel, "54321")) mustBe S4LSaved
    }

    "save the Employment VIEW model in BE if the model is complete" in new Setup {
      override def testNow = LocalDate.now

      when(mockPAYERegConnector.upsertEmployment(ArgumentMatchers.eq("54321"), ArgumentMatchers.eq(Fixtures.validEmploymentAPIModel))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validEmploymentAPIModel))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveEmploymentView(Fixtures.validEmploymentViewModel, "54321")) mustBe MongoSaved(Fixtures.validEmploymentViewModel)
    }

    "clear S4L data if the Employment VIEW model is saved in BE" in new Setup {
      override def testNow = LocalDate.now

      when(mockPAYERegConnector.upsertEmployment(ArgumentMatchers.eq("54321"), ArgumentMatchers.eq(Fixtures.validEmploymentAPIModel))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validEmploymentAPIModel))

      when(mockS4LService.clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveEmployment(Fixtures.validEmploymentViewModel, "54321"))
      verify(mockS4LService, times(1)).clear(ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier]())
    }
  }

  "calling saveEmployingStaff" should {
    "update the Employment VIEW model" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(Fixtures.incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveEmployingStaff(EmployingStaff(false), "54321")) mustBe Fixtures.incompleteEmploymentViewModel.copy(employing = Some(EmployingStaff(false)))
    }
  }

  "calling saveCompanyPension" should {
    "update the Employment VIEW model" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(Fixtures.incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveCompanyPension(CompanyPension(false), "54321")) mustBe Fixtures.incompleteEmploymentViewModel.copy(companyPension = Some(CompanyPension(false)))
    }
  }

  "calling saveSubcontractors" should {
    "update the Employment VIEW model" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(Fixtures.incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveSubcontractors(Subcontractors(false), "54321")) mustBe Fixtures.incompleteEmploymentViewModel.copy(subcontractors = Some(Subcontractors(false)))
    }
  }

  "calling saveFirstPayment" should {
    "update the Employment VIEW model" in new Setup {
      override def testNow = LocalDate.now

      when(mockS4LService.fetchAndGet(ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(Fixtures.incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](ArgumentMatchers.eq(CacheKeys.Employment.toString), ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      val date = LocalDate.of(2016, 12, 1)

      await(service.saveFirstPayment(FirstPaymentView(date), "54321")) mustBe Fixtures.incompleteEmploymentViewModel.copy(firstPayment = Some(FirstPaymentView(date)))
    }
  }

  "firstPaymentDateInNextYear" should {
    "return true" when {
      "now is before 6 Apr AND entered date is equal to 6 Apr" in new Setup {
        override def testNow = LocalDate.of(now.getYear,1,1)
        service.firstPaymentDateInNextYear(LocalDate.of(now.getYear,4,6)) mustBe true
      }

      "now is before 6 Apr AND entered date is after 6 Apr" in new Setup {
        override def testNow = LocalDate.of(now.getYear,1,1)
        service.firstPaymentDateInNextYear(LocalDate.of(now.getYear,4,7)) mustBe true
      }
    }

    "return false" when {
      "now is before 6 Apr AND entered date is before 6 Apr" in new Setup {
        override def testNow = LocalDate.of(now.getYear,1,1)
        service.firstPaymentDateInNextYear(LocalDate.of(now.getYear,1,1)) mustBe false
      }

      "now is after 6 Apr" in new Setup {
        override def testNow = LocalDate.of(now.getYear,4,10)
        service.firstPaymentDateInNextYear(LocalDate.of(now.getYear,4,15))
      }
    }
  }
}