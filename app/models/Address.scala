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

case class Address(line1: String,
                   line2: String,
                   line3: Option[String],
                   line4: Option[String],
                   postCode: Option[String],
                   country: Option[String] = None,
                   auditRef: Option[String] = None)

object Address {
  implicit val format = Json.format[Address]

  private val unitedKingdomDomains = List("United Kingdom", "UK", "GB", "Great Britain", "Wales", "Scotland", "Northern Ireland")

  def trimLine(stringToTrim: String, trimTo: Int): String = {
    val trimmed = stringToTrim.trim
    if(trimmed.length > trimTo) trimmed.substring(0, trimTo) else trimmed
  }

  def trimOptionalLine(stringToString: Option[String], trimTo: Int): Option[String] = {
    val trimmed = stringToString map(_.trim)
    trimmed map(_.substring(0, if(trimmed.get.length > trimTo) trimTo else trimmed.get.length))
  }

  val adressLookupReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {
      val unvalidatedPostCode = json.\("address").\("postcode").asOpt[String](Formatters.normalizeTrimmedReads)
      val validatedPostcode   = unvalidatedPostCode match {
        case Some(pc) if pc.matches(Validators.postcodeRegex) => Right(pc)
        case Some(_) => Left("Invalid postcode")
        case _ => Left("No postcode")
      }

      val addressLines  = json.\("address").\("lines").as[JsArray].as[List[String]](Formatters.normalizeTrimmedListReads)
      val countryName   = json.\("address").\("country").\("name").asOpt[String](Formatters.normalizeTrimmedReads)
      val auditRef      = json.\("auditRef").asOpt[String]

      def buildAddress: JsResult[Address] = (validatedPostcode, countryName, addressLines) match {
        case (Left(msg), None       , _    )                      => JsError(ValidationError(s"$msg and no country to default to"))
        case (_        , _          , lines) if lines.length < 2  => JsError(ValidationError(s"only ${lines.length} lines provided from address-lookup-frontend"))
        case (Left(_)  , c @ Some(_), lines)                      => JsSuccess(makeAddress(None, c, lines, auditRef))
        case (Right(pc), _          , lines)                      => JsSuccess(makeAddress(Some(pc), None, lines, auditRef))
      }

      countryName match {
        case Some(country) if !unitedKingdomDomains.contains(country) => JsSuccess(createForeignAddress(unvalidatedPostCode, countryName, addressLines, auditRef))
        case _ => buildAddress
      }
    }

    def createForeignAddress(postCode: Option[String], country: Option[String], lines: List[String], auditRef: Option[String]): Address = {
      val additionalLines = List(lines.lift(2), lines.lift(3), postCode).flatten
      Address(
        line1       = trimLine(lines.head, 27),
        line2       = trimLine(lines(1), 27),
        line3       = trimOptionalLine(additionalLines.headOption, 27),
        line4       = trimOptionalLine(additionalLines.lift(1),18),
        postCode    = None,
        country     = country,
        auditRef    = auditRef
      )
    }

    def makeAddress(postCode: Option[String], country: Option[String], lines: List[String], auditRef: Option[String]): Address = {
      Address(
        line1     = trimLine(lines.head, 27),
        line2     = trimLine(lines(1), 27),
        line3     = trimOptionalLine(lines.lift(2), 27),
        line4     = trimOptionalLine(lines.lift(3), 18),
        postCode  = postCode,
        country   = country,
        auditRef  = auditRef
      )
    }
  }

  val incorpInfoReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {

      val premises      = json.\("premises").as[String](Formatters.normalizeTrimmedReads)
      val addressLine1  = json.\("address_line_1").as[String](Formatters.normalizeTrimmedReads)
      val addressLine2  = json.\("address_line_2").asOpt[String](Formatters.normalizeTrimmedReads)
      val poBox         = json.\("po_box").asOpt[String](Formatters.normalizeTrimmedReads)
      val locality      = json.\("locality").as[String](Formatters.normalizeTrimmedReads)
      val region        = json.\("region").asOpt[String](Formatters.normalizeTrimmedReads)
      val postalCode    = json.\("postal_code").asOpt[String](Formatters.normalizeTrimmedReads)
      val country       = json.\("country").asOpt[String](Formatters.normalizeTrimmedReads)


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
        line1     = trimLine(line1, 27),
        line2     = trimLine(additionalLines.head, 27),
        line3     = trimOptionalLine(additionalLines.lift(1), 27),
        line4     = trimOptionalLine(additionalLines.lift(2), 18),
        postCode  = postalCode,
        country   = if(postalCode.isEmpty) country else None
      ))
    }
  }
}
