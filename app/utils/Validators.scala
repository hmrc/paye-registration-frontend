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

import models.view.UserEnteredNino
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{ValidationError, _}

object Validators extends DateUtil {

  private val emailRegex = """^(?!.{71,})([-0-9a-zA-Z.+_]+@[-0-9a-zA-Z.+_]+\.[a-zA-Z]{2,4})$"""
  private val emailLength = """[A-Za-z0-9\-_.@]{1,70}"""
  private val phoneNoTypeRegex = """^[0-9 ]{1,20}$""".r
  private val nonEmptyRegex = """^(?=\s*\S).*$""".r
  private val validNinoFormat = "[[a-zA-Z]&&[^DFIQUVdfiquv]][[a-zA-Z]&&[^DFIQUVOdfiquvo]] ?\\d{2} ?\\d{2} ?\\d{2} ?[a-dA-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  private val natureOfBusinessRegex = """^[A-Za-z 0-9\-,/&']{1,100}$""".r
  val postcodeRegex = """^[A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2}$"""
  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.toUpperCase.startsWith)

  def isValidNino(nino: String): Boolean = nino.nonEmpty && hasValidPrefix(nino) && nino.matches(validNinoFormat)

  def optionalValidation(constraint : Constraint[String]): Constraint[Option[String]] = Constraint("constraints.optional")({
    case Some(text: String)  if text != ""  => constraint(text)
    case _ => Valid
  })

  val emailValidation: Constraint[String] = Constraint("constraints.emailCheck")({
    text =>
      val errors = text.trim match {
        case wrong if !text.matches(emailRegex) => Seq(ValidationError("errors.invalid.email"))
        case tooLong if !text.matches(emailLength) => Seq(ValidationError("pages.businessContact.email.tooLong"))
        case _ => Nil
      }
      if(errors.isEmpty) Valid else Invalid(errors)
  })

  val phoneNumberValidation: Constraint[String] = Constraint("constraints.phoneNumberCheck")({
    text =>
      val errors = text.trim match {
        case phoneNoTypeRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.phoneNumber"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  val mobilePhoneNumberValidation: Constraint[String] = Constraint("constraints.mobilePhoneNumberCheck")({
    text =>
      val errors = text.trim match {
        case phoneNoTypeRegex() => Nil
        case _ => Seq(ValidationError("errors.invalid.mobileNumber"))
      }
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  def natureOfBusinessValidation: Mapping[String] = {
    val sicConstraint: Constraint[String] = Constraint("constraints.description")({
      text =>
        val errors = if(text.length >= 100) {
          Seq(ValidationError("errors.invalid.sic.overCharLimit"))
        } else {
          text.trim match {
            case natureOfBusinessRegex()  => Nil
            case ""                       => Seq(ValidationError("errors.invalid.sic.noEntry"))
            case _                        => Seq(ValidationError("errors.invalid.sic.invalidChars"))
          }
        }
        if(errors.isEmpty) Valid else Invalid(errors)
    })
    text().verifying(sicConstraint)
  }

  def firstPaymentDateWithinRange(date: LocalDate): Boolean = {
    lessOrEqualThanXDaysAfter(LocalDate.now(), date, 61)
  }
}


