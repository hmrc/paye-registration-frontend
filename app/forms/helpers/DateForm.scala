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

package forms.helpers

import java.time.LocalDate

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}
import utils.DateUtil

import scala.util.Try

trait DateForm extends DateUtil {

  val prefix: String

  def validation(dt: LocalDate): Either[Seq[FormError], LocalDate]

  implicit val dateFormatter = new Formatter[LocalDate] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

      val dte: Option[LocalDate] = Try(
        for {
          day   <- data.get(s"${prefix}Day")
          month <- data.get(s"${prefix}Month")
          year  <- data.get(s"${prefix}Year")
        } yield toDate(year, month, day)
      ).getOrElse(None)

      dte match {
        case Some(dt) => validation(dt)
        case None     => Left(Seq(FormError(s"${prefix}Day", "app.common.date.invalid")))
      }
    }

    override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
      s"${prefix}Day" -> value.getDayOfMonth.toString,
      s"${prefix}Month" -> value.getMonthValue.toString,
      s"${prefix}Year" -> value.getYear.toString
    )
  }

  val threePartDate: Mapping[LocalDate] = Forms.of[LocalDate](dateFormatter)
}
