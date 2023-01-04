/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.validation._

import java.time.LocalDate

object Validators extends DateUtil {

  private val emailRegex = """^(?!.{71,})([-0-9a-zA-Z.+_]+@[-0-9a-zA-Z.+_]+\.[a-zA-Z]{1,11})$"""
  private val validNinoFormat = "[[a-zA-Z]&&[^DFIQUVdfiquv]][[a-zA-Z]&&[^DFIQUVOdfiquvo]] ?\\d{2} ?\\d{2} ?\\d{2} ?[a-dA-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  private val natureOfBusinessRegex = """^(?![\r\n|\r|\n|\t])[A-Za-z 0-9\-,/&']{1,100}$"""
  val postcodeRegex = """^[A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2}$"""
  private val nameRegex = """^[A-Za-z 0-9\'-]{1,100}$""".r
  val minDate = LocalDate.of(1900, 1, 1)
  val desSessionRegex = "^[A-Za-z0-9-]{0,60}$"

  val datePatternRegex = """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})"""

  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.toUpperCase.startsWith)

  def isValidNino(nino: String): Boolean = nino.nonEmpty && hasValidPrefix(nino) && nino.matches(validNinoFormat)

  def optionalValidation(constraint: Constraint[String]): Constraint[Option[String]] = Constraint("constraints.optional")({
    case Some(text: String) if text != "" => constraint(text)
    case _ => Valid
  })

  def isValidPhoneNo(phone: String): Either[String, String] = {
    def isValidNumber(s: String) = s.replaceAll(" ", "").matches("[0-9]+")

    val digitCount = phone.trim.replaceAll(" ", "").length

    if (isValidNumber(phone) && digitCount > 20) {
      Left("errors.invalid.contactNum.tooLong")
    } else if (isValidNumber(phone) && digitCount < 10) {
      Left("errors.invalid.contactNum.tooShort")
    } else if (isValidNumber(phone)) {
      Right(phone.trim.replace(" ", ""))
    } else {
      Left("errors.invalid.contactNum")
    }
  }

  val emailValidation: Constraint[String] = Constraint("constraints.emailCheck")({ text =>
    val errors = if (text.trim.matches(emailRegex)) {
      Nil
    } else {
      text.trim match {
        case "" => Seq(ValidationError("errors.invalid.email.noEntry"))
        case _ if text.length > 70 => Seq(ValidationError("errors.invalid.email.tooLong"))
        case _ => Seq(ValidationError("errors.invalid.email"))
      }
    }
    if (errors.isEmpty) Valid else Invalid(errors)
  })

  val nameValidation: Constraint[String] = Constraint("constraints.nameCheck")({ text =>
    val errors = text.trim match {
      case name if name.length <= 0 => Seq(ValidationError("pages.payeContact.nameMandatory"))
      case nameRegex() => Nil
      case _ => Seq(ValidationError("errors.invalid.name.invalidChars"))
    }
    if (errors.isEmpty) Valid else Invalid(errors)
  })

  def isValidNatureOfBusiness(natureOfBusiness: String): Boolean = natureOfBusiness.matches(natureOfBusinessRegex)
}
