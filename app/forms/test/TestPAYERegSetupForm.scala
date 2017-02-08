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

package forms.test

import java.time.LocalDate

import forms.helpers.{DateForm, RequiredBooleanForm}
import models.api.{CompanyDetails, Employment, PAYERegistration}
import models.view.Address
import play.api.data.Form
import play.api.data.Forms._

object TestPAYERegSetupForm extends RequiredBooleanForm with DateForm {

  override val prefix = "employment.firstPayDate"
  override def validation(dt: LocalDate) = Right(dt)

  val form = Form(
    mapping(
      "registrationID" -> text,
      "formCreationTimestamp" -> text,
      "companyDetails" -> mapping(
        "crn" -> optional(text),
        "companyName" -> text,
        "tradingName" -> optional(text),
        "roAddress" -> mapping(
          "line1" -> text,
          "line2" -> text,
          "line3" -> optional(text),
          "line4" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)
        )(Address.apply)(Address.unapply)
      )(CompanyDetails.apply)(CompanyDetails.unapply),
      "employment" -> mapping(
        "employees" -> requiredBoolean,
        "companyPension" -> optional(requiredBoolean),
        "subcontractors" -> requiredBoolean,
        "firstPayDate" -> threePartDate
      )(Employment.apply)(Employment.unapply)
    )(PAYERegistration.apply)(PAYERegistration.unapply)
  )
}
