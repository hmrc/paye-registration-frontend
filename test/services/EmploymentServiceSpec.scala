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

import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import models.api.{Employment => EmploymentAPI, FirstPayment => FirstPaymentAPI}
import testHelpers.PAYERegSpec

class EmploymentServiceSpec extends PAYERegSpec with EmploymentService {
  "calling convertToModelAPI with EmployingStaff set as true" should {
    "return the corresponding converted Employment API Model with CompanyPension" in {
      val now = LocalDate.now()
      val viewModel = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(fromDate(now))))
      convertToModelAPI(viewModel) shouldBe Right(EmploymentAPI(true, Some(true), true, FirstPaymentAPI(now)))
    }
  }

  "calling convertToModelAPI with EmployingStaff set as false" should {
    "return the corresponding converted Employment API Model without CompanyPension" in {
      val now = LocalDate.now()
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(fromDate(now))))
      convertToModelAPI(viewModel) shouldBe Right(EmploymentAPI(false, None, true, FirstPaymentAPI(now)))
    }
  }

  "calling convertToModelAPI with EmployingStaff set as true and CompanyPension set as None" should {
    "return the Employment VIEW Model" in {
      val now = LocalDate.now()
      val viewModel = EmploymentView(Some(EmployingStaff(true)), None, Some(Subcontractors(true)), Some((FirstPaymentView.apply _).tupled(fromDate(now))))
      convertToModelAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling convertToModelAPI with Subcontractors set as None" should {
    "return the Employment VIEW Model" in {
      val now = LocalDate.now()
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, Some((FirstPaymentView.apply _).tupled(fromDate(now))))
      convertToModelAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling convertToModelAPI with FirstPayment set as None" should {
    "return the Employment VIEW Model" in {
      val now = LocalDate.now()
      val viewModel = EmploymentView(Some(EmployingStaff(false)), None, None, None)
      convertToModelAPI(viewModel) shouldBe Left(viewModel)
    }
  }

  "calling convertToModelView with EmployingStaff set as true" should {
    "return the corresponding converted Employment View Model with CompanyPension" in {
      val now = LocalDate.now()
      val apiModel = EmploymentAPI(true, Some(true), false, FirstPaymentAPI(now))
      convertToModelView(apiModel) shouldBe EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(false)), Some((FirstPaymentView.apply _).tupled(fromDate(now))))
    }
  }

  "calling convertToModelView with EmployingStaff set as false" should {
    "return the corresponding converted Employment View Model without CompanyPension" in {
      val now = LocalDate.now()
      val apiModel = EmploymentAPI(false, None, false, FirstPaymentAPI(now))
      convertToModelView(apiModel) shouldBe EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(false)), Some((FirstPaymentView.apply _).tupled(fromDate(now))))
    }
  }
}
