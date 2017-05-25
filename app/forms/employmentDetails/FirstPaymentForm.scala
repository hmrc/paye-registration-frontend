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

package forms.employmentDetails

import java.time.LocalDate

import forms.helpers.DateForm
import models.view.FirstPayment
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import utils.Validators

object FirstPaymentForm extends DateForm {

  override val prefix = "firstPay"
  override def validation(dt: LocalDate) = {
    if(Validators.firstPaymentDateWithinRange(dt)) Right(dt) else Left(Seq(FormError("firstPayDay", "pages.firstPayment.date.invalidRange")))
  }

  val form = Form(
    mapping(
    "firstPayDate" -> threePartDate
    )(FirstPayment.apply)(FirstPayment.unapply)
  )

}
