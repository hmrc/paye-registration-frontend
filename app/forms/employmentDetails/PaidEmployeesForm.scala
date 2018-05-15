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

package forms.employmentDetails

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import forms.helpers.{CustomDateForm, RequiredBooleanForm}
import models.view.EmployingAnyone
import play.api.data.{Form, FormError}
import play.api.data.Forms.{mapping, optional}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}
import utils.SystemDate

object PaidEmployeesForm extends RequiredBooleanForm with CustomDateForm {

  override val errorMsg = "pages.paidEmployees.error"
  override lazy val customFormPrefix = "earliestDate"
  def now: LocalDate = SystemDate.getSystemDate.toLocalDate

  val dateTimeFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  def isOnOrAfter(date: LocalDate, comparator: LocalDate): Boolean = date.isEqual(comparator) || date.isAfter(comparator)

  override def validation(dt: LocalDate, cdt: LocalDate) = {
    if (dt.isBefore(cdt)) {
      Left(Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.dateTooEarly", Seq(cdt.format(dateTimeFormat)))))
    } else if (isOnOrAfter(dt, cdt) && !isOnOrAfter(dt, now.minusYears(2))) {
      Left(Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.moreThanTwoYears")))
    } else if (dt.isAfter(now)) {
      Left(Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.dateInFuture")))
    } else {
      Right(dt)
    }
  }

  def form(date: LocalDate) = {
    Form(
      mapping(
        "alreadyPaying" -> requiredBoolean,
        "earliestDate" -> mandatoryIf(isEqual("alreadyPaying", "true"), threePartDateWithComparison(date))
      )(EmployingAnyone.apply)(EmployingAnyone.unapply)
    )
  }
}
