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


import forms.helpers.BooleanForm
import models.view.WillBePaying
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

object EmployingStaffFormV2 extends BooleanForm {
  val form = Form(
    mapping(
      "willBePaying" -> requiredBoolean("pages.willBePaying.empty"),
      "beforeNewTaxYear" -> mandatoryIf(isEqual("willBePaying", "true"), requiredBoolean("pages.willBePaying.beforeNewTaxYear.empty"))
    )(WillBePaying.apply)(WillBePaying.unapply)
  )
}