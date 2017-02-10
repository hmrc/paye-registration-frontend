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
import play.api.data.validation._

object Validators extends DateUtil {

  private val emailRegex = """[A-Za-z0-9\-_.@]{1,70}""".r
  private val phoneRegex = """[0-9 ]{1,20}""".r
  private val mobileRegex = """[0-9 ]{1,20}""".r

  def optionalValidation(constraint : Constraint[String]): Constraint[Option[String]] = Constraint("constraints.optional")({
    case Some(text: String)  if text != ""  => constraint(text)
    case _ => Valid
  })

  val emailValidation: Constraint[String] = Constraint("constraints.emailCheck")({
    text =>
      val errors = text match {
        case emailRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.email"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  val phoneNumberValidation: Constraint[String] = Constraint("constraints.phoneNumberCheck")({
    text =>
      val errors = text match {
        case phoneRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.phoneNumber"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  val mobilePhoneNumberValidation: Constraint[String] = Constraint("constraints.mobilePhoneNumberCheck")({
    text =>
      val errors = text match {
        case mobileRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.mobileNumber"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  def firstPaymentDateWithinRange(date: LocalDate): Boolean = {
    lessOrEqualThanXDaysAfter(LocalDate.now(), date, 61)
  }
}


