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
import utils.{Formatters, Validators}

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

      val validatedPostcode = json.\("address").\("postcode").asOpt[String](Formatters.normalizeReads) match {
        case Some(pc) if pc.matches(Validators.postcodeRegex) => Right(pc)
        case Some(pc) => Left("Invalid postcode")
        case _ => Left("No postcode")
      }

      val addressLines = json.\("address").\("lines").as[JsArray].as[List[String]](Formatters.normalizeListReads)
      val countryName = json.\("address").\("country").\("name").asOpt[String](Formatters.normalizeReads)

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
      Address(
        line1     = lines.head.substring(0,if(lines.head.length > 27) 27 else lines.head.length),
        line2     = lines(1).substring(0,if(lines(1).length > 27) 27 else lines(1).length),
        line3     = L3.map(_.substring(0,if(L3.get.length > 27) 27 else L3.get.length)),
        line4     = L4.map(_.substring(0,if(L4.get.length > 18) 18 else L4.get.length)),
        postCode  = postCode,
        country   = country
      )
    }
  }

  val incorpInfoReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {

      val premises = json.\("premises").as[String](Formatters.normalizeReads)
      val addressLine1 = json.\("address_line_1").as[String](Formatters.normalizeReads)
      val addressLine2 = json.\("address_line_2").asOpt[String](Formatters.normalizeReads)
      val poBox = json.\("po_box").asOpt[String](Formatters.normalizeReads)
      val locality = json.\("locality").as[String](Formatters.normalizeReads)
      val region = json.\("region").asOpt[String](Formatters.normalizeReads)
      val postalCode = json.\("postal_code").asOpt[String](Formatters.normalizeReads)
      val country = json.\("country").asOpt[String](Formatters.normalizeReads)


      val (line1, oLine2) = if((premises + " " + addressLine1).length > 26) {
        (premises, Some(addressLine1))
      } else {
        (premises + " " + addressLine1, None)
      }

      val addrLine2POBox: Option[String] = Seq(addressLine2, poBox).flatten.foldLeft("")(_ + " " + _).trim match {
        case ""  => None
        case str  => Some(str)
      }

      val additionalLines: Seq[String] = Seq(oLine2, addrLine2POBox, Some(locality), region).flatten

      JsSuccess(Address(
        line1,
        additionalLines.head,
        additionalLines.lift(1),
        additionalLines.lift(2),
        postalCode,
        if(postalCode.isEmpty) country else None
      ))
    }
  }
}
