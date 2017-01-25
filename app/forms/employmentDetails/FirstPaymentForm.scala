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
import java.time.temporal.ChronoUnit

import models.view.FirstPayment
import utils.DateUtil
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import utils.Validators._

object FirstPaymentForm extends DateUtil {
  val form = Form(
    mapping(
      "firstPayYear" -> nonEmptyText,
      "firstPayMonth" -> nonEmptyText,
      "firstPayDay" -> nonEmptyText
    )(FirstPayment.apply)(FirstPayment.unapply).verifying(StopOnFirstFail(isInvalidDate, firstPaymentDateRange))
  )
}
