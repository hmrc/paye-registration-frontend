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

package models.view

import models.{Address, DigitalContactDetails}
import play.api.libs.json._
import utils.Formatters

case class PAYEContactDetails(name: String,
                              digitalContactDetails: DigitalContactDetails)

object PAYEContactDetails {
  implicit val digitalContactFormat = DigitalContactDetails.format
  implicit val format = Json.format[PAYEContactDetails]

  val prepopFormat: Reads[PAYEContactDetails] = new Reads[PAYEContactDetails] {
    def reads(json: JsValue): JsResult[PAYEContactDetails] = {
      val oFirstName = json.\("firstName").asOpt[String](Formatters.normalizeTrimmedReads)
      val oMiddleName = json.\("middleName").asOpt[String](Formatters.normalizeTrimmedReads)
      val oLastName = json.\("middleName").asOpt[String](Formatters.normalizeTrimmedReads)
      val oEmail = json.\("email").asOpt[String](Formatters.normalizeTrimmedReads)
      val oPhone = json.\("telephoneNumber").asOpt[String](Formatters.normalizeTrimmedReads)
      val oMobile = json.\("mobileNumber").asOpt[String](Formatters.normalizeTrimmedReads)

      def incorrectContactDetails(msg: String): String = {
        s"$msg\n" +
          s"Lines defined:\n" +
          s"firstName: ${oFirstName.isDefined}\n" +
          s"middleName: ${oMiddleName.isDefined}\n" +
          s"lastName: ${oLastName.isDefined}\n" +
          s"email: ${oEmail.isDefined}\n" +
          s"mobile: ${oMobile.isDefined}\n" +
          s"phone: ${oPhone.isDefined}\n"
      }

      if(oFirstName.isEmpty && oMiddleName.isEmpty && oLastName.isEmpty) {
        JsError(incorrectContactDetails(s"No name components defined"))
      } else if (oEmail.isEmpty && oMobile.isEmpty && oPhone.isEmpty) {
        JsError(incorrectContactDetails(s"No contact details defined"))
      } else {
        JsSuccess(
          PAYEContactDetails(
            name                  = Seq(oFirstName, oMiddleName, oLastName).flatten.mkString(" "),
            digitalContactDetails = DigitalContactDetails(oEmail, oMobile, oPhone))
        )
      }
    }
  }
}

case class PAYEContact(contactDetails: Option[PAYEContactDetails],
                       correspondenceAddress: Option[Address])

object PAYEContact {
  implicit val digitalContactFormat = DigitalContactDetails.format
  implicit val format = Json.format[PAYEContact]
}
