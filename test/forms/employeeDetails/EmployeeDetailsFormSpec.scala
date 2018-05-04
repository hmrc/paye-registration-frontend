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

package forms.employeeDetails

import java.time.LocalDate

import forms.employmentDetails.PaidEmployeesForm
import helpers.PayeComponentSpec
import models.view.EmployingAnyone

class EmployeeDetailsFormSpec extends PayeComponentSpec {

  "PaidEmployeesForm" should {
    "return an EmployAnyone(false, None)" in {
      val value = Map("alreadyPaying" -> "false")
      PaidEmployeesForm.form(LocalDate.now().minusMonths(2)).bind(value).value.get mustBe EmployingAnyone(false, None)
    }
  }
}
