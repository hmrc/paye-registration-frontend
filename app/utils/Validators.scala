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
import play.api.data.validation._

object Validators extends DateUtil {

  private val emailRegex = """^[A-Za-z0-9\-_.@]{1,70}$""".r
  private val phoneRegex = """^[0-9 ]{1,20}$""".r
  private val mobileRegex = """^[0-9 ]{1,20}$""".r
  private val ninoRegex = """^(AA|AB|AE|AH|AK|AL|AM|AP|AR|AS|AT|AW|AX|AY|AZ|BA|BB|BE|BH|BK|BL|BM|BT|CA|CB|CE|CH|CK|CL|CR|EA|EB|EE|EH|EK|EL|EM|EP|ER|ES|ET|EW|EX|EY|EZ|GY|HA|HB|HE|HH|HK|HL|HM|HP|HR|HS|HT|HW|HX|HY|HZ|JA|JB|JC|JE|JG|JH|JJ|JK|JL|JM|JN|JP|JR|JS|JT|JW|JX|JY|JZ|KA|KB|KE|KH|KK|KL|KM|KP|KR|KS|KT|KW|KX|KY|KZ|LA|LB|LE|LH|LK|LL|LM|LP|LR|LS|LT|LW|LX|LY|LZ|MA|MW|MX|NA|NB|NE|NH|NL|NM|NP|NR|NS|NW|NX|NY|NZ|OA|OB|OE|OH|OK|OL|OM|OP|OR|OS|OX|PA|PB|PC|PE|PG|PH|PJ|PK|PL|PM|PN|PP|PR|PS|PT|PW|PX|PY|RA|RB|RE|RH|RK|RM|RP|RR|RS|RT|RW|RX|RY|RZ|SA|SB|SC|SE|SG|SH|SJ|SK|SL|SM|SN|SP|SR|SS|ST|SW|SX|SY|SZ|TA|TB|TE|TH|TK|TL|TM|TP|TR|TS|TT|TW|TX|TY|TZ|WA|WB|WE|WK|WL|WM|WP|YA|YB|YE|YH|YK|YL|YM|YP|YR|YS|YT|YW|YX|YY|YZ|ZA|ZB|ZE|ZH|ZK|ZL|ZM|ZP|ZR|ZS|ZT|ZW|ZX|ZY)[0-9]{6}[A-DFM]{1}$""".r

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

  val ninoValidation: Constraint[UserEnteredNino] = Constraint("constraints.ninoCheck")({
    userNino =>
      val errors = userNino.nino.map{
        case ninoRegex(_) => Nil
        case _ => Seq(ValidationError("errors.invalid.nino"))
      }.getOrElse(Nil)
      if (errors.isEmpty) Valid else Invalid(errors)
  })

  def firstPaymentDateWithinRange(date: LocalDate): Boolean = {
    lessOrEqualThanXDaysAfter(LocalDate.now(), date, 61)
  }
}


