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

package utils

import java.time.LocalDate
import models.view.FirstPayment
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import scala.util.Try

object Validators extends DateUtil {
  def isInvalidDate: Constraint[FirstPayment] = {
    Constraint("constraints.validateDate")({
      model => {
        val date = Try(toDate(model.firstPayYear, model.firstPayMonth, model.firstPayDay))
        if( date.isFailure ) Invalid(Seq(ValidationError("pages.firstPayment.date.invalid", "firstPayDay"))) else Valid
      }
    })
  }


  def firstPaymentDateRange: Constraint[FirstPayment] = {
    Constraint("constraints.firstPaymentDateRange")({
      model => {
        val date = toDate(model.firstPayYear, model.firstPayMonth, model.firstPayDay)
        if( !lessOrEqualThanXDaysAfter(LocalDate.now(), date, 61) ) Invalid(Seq(ValidationError("pages.firstPayment.date.invalidRange", "firstPayDay"))) else Valid
      }
    })
  }
}


