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

import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.Validators

case class Address(
                    line1: String,
                    line2: String,
                    line3: Option[String],
                    line4: Option[String],
                    postCode: Option[String],
                    country: Option[String] = None
                  )

object Address {
  implicit val format = Json.format[Address]

  val adressLookupReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {

      val validatedPostcode = json.\("address").\("postcode").asOpt[String] match {
        case Some(pc) if pc.matches(Validators.postcodeRegex) => Right(pc)
        case Some(pc) => Left("Invalid postcode")
        case _ => Left("No postcode")
      }

      val addressLines = json.\("address").\("lines").as[JsArray].as[List[String]]
      val countryName = json.\("address").\("country").\("name").asOpt[String]

      (validatedPostcode, countryName, addressLines) match {
        case (Left(msg), None, _)              => JsError(ValidationError(s"$msg and no country to default to"))
        case (_, _, lines) if lines.length < 2 => JsError(ValidationError(s"only ${lines.length} lines provided from address-lookup-frontend"))
        case (Left(msg), c @ Some(_), lines)   => JsSuccess(makeAddress(None, c, lines))
        case (Right(pc), _, lines)             => JsSuccess(makeAddress(Some(pc), None, lines))
      }
    }

    def makeAddress(postCode: Option[String], country: Option[String], lines: List[String]) = {
      val L3 = if(lines.isDefinedAt(2)) Some(lines(2)) else None
      val L4 = if(lines.isDefinedAt(3)) Some(lines(3)) else None
      Address(lines.head,lines(1),L3,L4,postCode,country)
    }
  }
}
