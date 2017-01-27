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

import connectors.PAYERegistrationConnector
import enums.CacheKeys
import fixtures.{PAYERegistrationFixture, S4LFixture}
import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import models.api.{Employment => EmploymentAPI}
import org.mockito.Matchers
import org.mockito.Mockito.when
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
    val service = new EmploymentService {
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector
      override val s4LService = mockS4LService
    }

    val dateUtil = new DateUtil {}
  }

  "calling viewToAPI with EmployingStaff set as true" should {
    "return the corresponding converted Employment API Model with CompanyPension" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
      service.viewToAPI(viewModel) shouldBe Right(EmploymentAPI(true, Some(true), true, now))
    }
  }

  "calling viewToAPI with EmployingStaff set as false" should {
    "return the corresponding converted Employment API Model without CompanyPension" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
      service.viewToAPI(viewModel) shouldBe Right(EmploymentAPI(false, None, true, now))
    }
  }

  "calling viewToAPI with EmployingStaff set as true and CompanyPension set as None" should {
    "return the Employment VIEW Model" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(true)), None, Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling viewToAPI with Subcontractors set as None" should {
    "return the Employment VIEW Model" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling viewToAPI with FirstPayment set as None" should {
    "return the Employment VIEW Model" in new Setup {
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, None)
      service.viewToAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling apiToView with EmployingStaff set as true" should {
    "return the corresponding converted Employment View Model with CompanyPension" in new Setup {
      val apiModel = EmploymentAPI(true, Some(true), false, now)
      val expected = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(false)), Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
      service.apiToView(apiModel) shouldBe expected
    }
  }

  "calling apiToView with EmployingStaff set as false" should {
    "return the corresponding converted Employment View Model without CompanyPension" in new Setup {
      val apiModel = EmploymentAPI(false, None, false, now)
      service.apiToView(apiModel) shouldBe EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(false)), Some((FirstPaymentView.apply _).tupled(dateUtil.fromDate(now))))
    }
  }

  "calling fetchEmploymentView" should {
    "return the Employment VIEW model if found in S4L" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.Employment.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(validEmploymentViewModel)))

      await(service.fetchEmploymentView()) shouldBe Some(validEmploymentViewModel)
    }

    "return the Employment VIEW model from the connector if not found in S4L" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getEmployment(Matchers.contains("54321"))(Matchers.any[HeaderCarrier](), Matchers.any()))
        .thenReturn(Future.successful(Some(validEmploymentAPIModel)))

      await(service.fetchEmploymentView()) shouldBe Some(validEmploymentViewModel)
    }

    "return an empty Employment VIEW model if not found in S4L or in connector" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.Employment.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(None))

      when(mockPAYERegConnector.getEmployment(Matchers.contains("54321"))(Matchers.any[HeaderCarrier](), Matchers.any()))
        .thenReturn(Future.successful(None))

      await(service.fetchEmploymentView()) shouldBe Some(EmploymentView(None, None, None, None))
    }
  }

  "calling saveEmploymentView" should {
    "save the Employment VIEW model in S4L if the model is incomplete" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.Employment.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(incompleteEmploymentViewModel)))

      when(mockS4LService.saveForm[EmploymentView](Matchers.eq(CacheKeys.Employment.toString), Matchers.any())(Matchers.any[HeaderCarrier](), Matchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(returnCacheMap))

      await(service.saveEmploymentView("54321")) shouldBe S4LSaved
    }

    "save the Employment VIEW model in BE if the model is complete" in new Setup {
      mockFetchRegID("54321")
      when(mockS4LService.fetchAndGet(Matchers.eq(CacheKeys.Employment.toString))(Matchers.any[HeaderCarrier](), Matchers.any[Format[EmploymentView]]()))
        .thenReturn(Future.successful(Some(validEmploymentViewModel)))

      when(mockPAYERegConnector.upsertEmployment(Matchers.eq("54321"), Matchers.eq(validEmploymentAPIModel))(Matchers.any[HeaderCarrier](), Matchers.any()))
        .thenReturn(Future.successful(validEmploymentAPIModel))

      when(mockS4LService.clear()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(returnHttpResponse))

      await(service.saveEmploymentView("54321")) shouldBe MongoSaved
    }
  }
}
