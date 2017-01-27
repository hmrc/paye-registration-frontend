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

import models.view.{CompanyPension, EmployingStaff, Subcontractors, Employment => EmploymentView, FirstPayment => FirstPaymentView}
import models.api.{Employment => EmploymentAPI}
import utils.DateUtil

trait EmploymentService extends DateUtil {
  def convertToModelAPI(viewData: EmploymentView): Either[EmploymentView, EmploymentAPI] = viewData match {
    case EmploymentView(Some(EmployingStaff(true)), Some(pension), Some(cis), Some(pay)) =>
      Right(EmploymentAPI(true, Some(pension.pensionProvided), cis.hasContractors, toDate(pay.firstPayYear, pay.firstPayMonth, pay.firstPayDay)))
    case EmploymentView(Some(EmployingStaff(false)), _, Some(cis), Some(pay)) =>
      Right(EmploymentAPI(false, None, cis.hasContractors, toDate(pay.firstPayYear, pay.firstPayMonth, pay.firstPayDay)))
    case _ => Left(viewData)
  }

  def convertToModelView(apiData: EmploymentAPI): EmploymentView = apiData match {
    case EmploymentAPI(true, Some(pensionProvided), hasContractors, paymentDate) =>
      EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(pensionProvided)), Some(Subcontractors(hasContractors)), Some((FirstPaymentView.apply _).tupled(fromDate(paymentDate))))
    case EmploymentAPI(false, _, hasContractors, paymentDate) =>
      EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(hasContractors)), Some((FirstPaymentView.apply _).tupled(fromDate(paymentDate))))
  }
}
