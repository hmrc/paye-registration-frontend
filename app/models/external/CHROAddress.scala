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

package models.external

import models.view.Address
import play.api.libs.functional.syntax._
import play.api.libs.json.__

case class CHROAddress(
                        premises: String,
                        addressLine1: String,
                        addressLine2: Option[String],
                        locality: String,
                        country: Option[String],
                        poBox: Option[String],
                        postalCode: Option[String],
                        region: Option[String]
                      ) {
  implicit def convertToAddress: Address = Address(
    s"${this.premises} ${this.addressLine1}",
    this.locality,
    this.addressLine2,
    this.poBox,
    this.postalCode,
    this.country
  )
}

object CHROAddress {
  implicit val formatModel = (
    (__ \ "premises").format[String] and
      (__ \ "address_line_1").format[String] and
      (__ \ "address_line_2").formatNullable[String] and
      (__ \ "locality").format[String] and
      (__ \ "country").formatNullable[String] and
      (__ \ "po_box").formatNullable[String] and
      (__ \ "postal_code").formatNullable[String] and
      (__ \ "region").formatNullable[String]
    )(CHROAddress.apply, unlift(CHROAddress.unapply))
}
