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

package models

import play.api.libs.json.{JsObject, Json, Writes}

case class DigitalContactDetails(email : Option[String],
                                  mobileNumber : Option[String],
                                  phoneNumber : Option[String])

object DigitalContactDetails {
  implicit val format = Json.format[DigitalContactDetails]

  val prepopWrites: Writes[DigitalContactDetails] = new Writes[DigitalContactDetails] {
    def writes(contactDetails: DigitalContactDetails): JsObject = {
      val jsonEmail = contactDetails.email.fold(Json.obj())(email => Json.obj("email" -> email))
      val jsonMobileNumber = contactDetails.mobileNumber.fold(Json.obj())(mobile => Json.obj("mobileNumber" -> mobile))
      val jsonTelephoneNumber = contactDetails.phoneNumber.fold(Json.obj())(tel => Json.obj("telephoneNumber" -> tel))

      jsonEmail ++ jsonMobileNumber ++ jsonTelephoneNumber
    }
  }
}
