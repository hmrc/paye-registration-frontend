/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.helpers.CustomDateForm
import forms.test.TestPAYERegSetupForm.requiredBoolean
import models.api.{Employing, Employment}
import play.api.data.Forms.{mapping, _}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}
import utils.SystemDate

object TestPAYERegEmploymentInfoSetupForm extends CustomDateForm {
  override val customFormPrefix = "earliestDate"

  override def validation(dt: LocalDate, cdt: LocalDate) = Right(dt)

  def now: LocalDate = SystemDate.getSystemDate.toLocalDate

  implicit def employingStatusFormatter: Formatter[Employing.Value] = new Formatter[Employing.Value] {
    def bind(key: String, data: Map[String, String]) = {
      Right(data.getOrElse(key, "")).right.flatMap {
        case "alreadyEmploying" => Right(Employing.alreadyEmploying)
        case "willEmployNextYear" => Right(Employing.willEmployNextYear)
        case "willEmployThisYear" => Right(Employing.willEmployThisYear)
        case "notEmploying" => Right(Employing.notEmploying)
        case _ => Left(Seq(FormError(key, "error.required", Nil)))
      }
    }

    def unbind(key: String, value: Employing.Value) = Map(key -> value.toString)
  }

  val employingStatus: Mapping[Employing.Value] = Forms.of[Employing.Value](employingStatusFormatter)

  val form = Form(
    mapping(
      "employees" -> employingStatus,
      "earliestDate" -> threePartDateWithComparison(now),
      "cis" -> requiredBoolean,
      "subcontractors" -> requiredBoolean,
      "pensions" -> optional(requiredBoolean)
    )(Employment.apply)(Employment.unapply)
  )
}
