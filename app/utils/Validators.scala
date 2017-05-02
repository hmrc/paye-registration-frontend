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

  private val emailRegex = """^([A-Za-z0-9\-_.]+)@([A-Za-z0-9\-_.]+)\.[A-Za-z0-9\-_.]{2,3}$"""
  private val phoneNoTypeRegex = """^[0-9 ]{1,20}$""".r
  private val nonEmptyRegex = """^(?=\s*\S).*$""".r
  private val validNinoFormat = "[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  private val natureOfBusinessRegex = """^[A-Za-z 0-9\-,/&']{1,100}$""".r
  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.startsWith)

  def isValidNino(nino: String): Boolean = nino.nonEmpty && hasValidPrefix(nino) && nino.matches(validNinoFormat)

  def optionalValidation(constraint : Constraint[String]): Constraint[Option[String]] = Constraint("constraints.optional")({
    case Some(text: String)  if text != ""  => constraint(text)
    case _ => Valid
  })

  val emailValidation: Constraint[String] = Constraint("constraints.emailCheck")({
    text =>
      val errors = text.trim match {
        case tooLong if text.length > 70 => Seq(ValidationError("pages.businessContact.email.tooLong"))
        case wrong if !text.matches(emailRegex) => Seq(ValidationError("errors.invalid.email"))
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


