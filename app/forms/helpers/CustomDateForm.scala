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

package forms.helpers

import java.time.LocalDate

import play.api.data.{FormError, Forms, Mapping}
import play.api.data.format.Formatter
import utils.DateUtil

import scala.util.Try

trait CustomDateForm extends DateUtil {
  val customFormPrefix: String

  def validation(dt: LocalDate, cdt: LocalDate): Either[Seq[FormError], LocalDate]

  def dateFormatter(date: LocalDate) = new Formatter[LocalDate] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
      (data.get(s"${customFormPrefix}Day"), data.get(s"${customFormPrefix}Month"), data.get(s"${customFormPrefix}Year")) match {
        case (a,b,c) if (a :: b :: c :: Nil).collect{case Some("") | None => false}.contains(false) => Left(Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.empty")))
        case (Some(day), Some(month), Some(year)) =>
          Try(toDate(year, month, day)).toOption match {
            case Some(dt) => validation(dt, date)
            case None     => Left(Seq(FormError(s"${customFormPrefix}-fieldset", "pages.paidEmployees.date.invalid")))
          }
      }
    }

    override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
      s"${customFormPrefix}Day"   -> value.getDayOfMonth.toString,
      s"${customFormPrefix}Month" -> value.getMonthValue.toString,
      s"${customFormPrefix}Year"  -> value.getYear.toString
    )
  }

  def threePartDateWithComparison(date: LocalDate): Mapping[LocalDate] = Forms.of[LocalDate](dateFormatter(date))
}