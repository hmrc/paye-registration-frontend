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

import play.api.data.validation.{ValidationError, _}

object Validators extends DateUtil {

  private val emailRegex = """^(?!.{71,})([-0-9a-zA-Z.+_]+@[-0-9a-zA-Z.+_]+\.[a-zA-Z]{2,4})$"""
  private val phoneNoTypeRegex = """^[0-9 ]{1,20}$""".r
  private val nonEmptyRegex = """^(?=\s*\S).*$""".r
  private val validNinoFormat = "[[a-zA-Z]&&[^DFIQUVdfiquv]][[a-zA-Z]&&[^DFIQUVOdfiquvo]] ?\\d{2} ?\\d{2} ?\\d{2} ?[a-dA-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  private val natureOfBusinessRegex = """^(?![\r\n|\r|\n|\t])[A-Za-z 0-9\-,/&']{1,100}$"""
  val postcodeRegex = """^[A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2}$"""
  private val nameRegex = """^[A-Za-z 0-9\'-]{1,100}$""".r
  val minDate = LocalDate.of(1900,1,1)

  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.toUpperCase.startsWith)

  def isValidNino(nino: String): Boolean = nino.nonEmpty && hasValidPrefix(nino) && nino.matches(validNinoFormat)

  def optionalValidation(constraint : Constraint[String]): Constraint[Option[String]] = Constraint("constraints.optional")({
    case Some(text: String)  if text != ""  => constraint(text)
    case _ => Valid
  })

  def isValidPhoneNo(phone: String, msgError: String): Either[String, String] = {
    def isValidNumberCount(s: String) = s.replaceAll(" ", "").matches("[0-9]{10,20}")

    (isValidNumberCount(phone), phone.trim.matches(phoneNoTypeRegex.toString)) match {
      case (true, true) => Right(phone.trim)
      case (true, false) => Right(phone.replaceAll(" ", ""))
      case (false, _) => Left(msgError)
    }
  }

  val emailValidation: Constraint[String] = Constraint("constraints.emailCheck")({
    text =>
      val errors = text.trim match {
        case tooLong if text.length >= 70       => Seq(ValidationError("errors.invalid.email.tooLong"))
        case wrong if !text.matches(emailRegex) => Seq(ValidationError("errors.invalid.email"))
        case _ => Nil
      }
      if(errors.isEmpty) Valid else Invalid(errors)
  })

  val nameValidation: Constraint[String] = Constraint("constraints.nameCheck")({
    text =>
      val errors = text.trim match {
        case name if name.length <= 0 => Seq(ValidationError("pages.payeContact.nameMandatory"))
        case nameRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.name.invalidChars"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  def isValidNatureOfBusiness(natureOfBusiness: String): Boolean = natureOfBusiness.matches(natureOfBusinessRegex)

  def firstPaymentDateWithinRange(date: LocalDate): Boolean = {
    lessOrEqualThanXDaysAfter(LocalDate.now(), date, 61)
  }

  def beforeMinDate(date: LocalDate): Boolean = {
    date.isBefore(minDate)
  }
}


