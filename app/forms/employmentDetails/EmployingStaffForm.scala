/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.helpers.BooleanForm
import models.view.WillBePaying
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

object EmployingStaffForm extends BooleanForm {
  def isRequiredBeforeNewTaxYear(now: LocalDate): Boolean = now.isAfter(LocalDate.of(now.getYear, 2, 5)) && now.isBefore(LocalDate.of(now.getYear, 4, 6))

  private def beforeNewTaxYearMapping(now: LocalDate): Mapping[Option[Boolean]] =
    if (isRequiredBeforeNewTaxYear(now)) mandatoryIf(isEqual("willBePaying", "true"), requiredBoolean("pages.willBePaying.beforeNewTaxYear.empty")) else ignored[Option[Boolean]](None)

  def form(now: LocalDate) = Form(
    mapping(
      "willBePaying" -> requiredBoolean("pages.willBePaying.empty"),
      "beforeNewTaxYear" -> beforeNewTaxYearMapping(now)
    )(WillBePaying.apply)(WillBePaying.unapply)
  )
}