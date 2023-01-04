/*
 * Copyright 2023 HM Revenue & Customs
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

import models.Address.prePopReads
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.{Formatters, Validators}

case class Address(line1: String,
                   line2: String,
                   line3: Option[String],
                   line4: Option[String],
                   postCode: Option[String],
                   country: Option[String] = None,
                   auditRef: Option[String] = None) {

 override def toString: String = Seq(Some(line1), Some(line2), line3, line4, postCode, country).flatten.mkString(", ")

}

object Address {
  implicit val format = Json.format[Address]

  private val unitedKingdomDomains = List("United Kingdom", "UK", "GB", "Great Britain", "Wales", "Scotland", "Northern Ireland")

  def trimLine(stringToTrim: String, trimTo: Int): String = {
    val trimmed = stringToTrim.trim
    if (trimmed.length > trimTo) trimmed.substring(0, trimTo) else trimmed
  }

  def trimOptionalLine(stringToString: Option[String], trimTo: Int): Option[String] = {
    val trimmed = stringToString map (_.trim)
    trimmed map (_.substring(0, if (trimmed.get.length > trimTo) trimTo else trimmed.get.length))
  }

  def validatePostcode(unvalidatedPostcode: Option[String]): Either[String, String] = {
    unvalidatedPostcode match {
      case Some(pc) if pc.matches(Validators.postcodeRegex) => Right(pc)
      case Some(_) => Left("Invalid postcode")
      case _ => Left("No postcode")
    }
  }

  val addressLookupReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {
      val unvalidatedPostCode = json.\("address").\("postcode").asOpt[String](Formatters.normalizeTrimmedReads)
      val validatedPostcode = validatePostcode(unvalidatedPostCode)

      val addressLines = json.\("address").\("lines").as[JsArray].as[List[String]](Formatters.normalizeTrimmedListReads)
      val countryName = json.\("address").\("country").\("name").asOpt[String](Formatters.normalizeTrimmedReads)
      val auditRef = json.\("auditRef").asOpt[String]

      def buildAddress: JsResult[Address] = (validatedPostcode, countryName, addressLines) match {
        case (Left(msg), None, _) => JsError(JsonValidationError(s"$msg and no country to default to"))
        case (_, _, lines) if lines.length < 2 => JsError(JsonValidationError(s"only ${lines.length} lines provided from address-lookup-frontend"))
        case (Left(_), c@Some(_), lines) => JsSuccess(makeAddress(None, c, lines, auditRef))
        case (Right(pc), _, lines) => JsSuccess(makeAddress(Some(pc), None, lines, auditRef))
      }

      countryName match {
        case Some(country) if !unitedKingdomDomains.contains(country) => JsSuccess(createForeignAddress(unvalidatedPostCode, countryName, addressLines, auditRef))
        case _ => buildAddress
      }
    }

    def createForeignAddress(postCode: Option[String], country: Option[String], lines: List[String], auditRef: Option[String]): Address = {
      val additionalLines = List(lines.lift(2), lines.lift(3), postCode).flatten
      Address(
        line1 = trimLine(lines.head, 27),
        line2 = trimLine(lines(1), 27),
        line3 = trimOptionalLine(additionalLines.headOption, 27),
        line4 = trimOptionalLine(additionalLines.lift(1), 18),
        postCode = None,
        country = country,
        auditRef = auditRef
      )
    }

    def makeAddress(postCode: Option[String], country: Option[String], lines: List[String], auditRef: Option[String]): Address = {
      Address(
        line1 = trimLine(lines.head, 27),
        line2 = trimLine(lines(1), 27),
        line3 = trimOptionalLine(lines.lift(2), 27),
        line4 = trimOptionalLine(lines.lift(3), 18),
        postCode = postCode,
        country = country,
        auditRef = auditRef
      )
    }
  }

  val incorpInfoReads: Reads[Address] = new Reads[Address] {
    def reads(json: JsValue): JsResult[Address] = {

      val oPremises = json.\("premises").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)
      val oAddressLine1 = json.\("address_line_1").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)
      val oAddressLine2 = json.\("address_line_2").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)
      val oLocality = json.\("locality").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)
      val oPostalCode = json.\("postal_code").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)
      val oCountry = json.\("country").asOpt[String](Formatters.normalizeTrimmedHMRCAddressReads)


      val (oLine1, oLine2) = oPremises match {
        case Some(premises) => oAddressLine1 match {
          case Some(addressLine1) => if ((premises + " " + addressLine1).length > 26) {
            (Some(premises), Some(addressLine1))
          } else {
            (Some(premises + " " + addressLine1), None)
          }
          case None => (Some(premises), None)
        }
        case None => (oAddressLine1, None)
      }

      val lines: Seq[String] = Seq(oLine1, oLine2, oAddressLine2, oLocality).flatten

      def incorrectAddressErrorMessage(msg: String): String = {
        s"$msg\n" +
          s"Lines defined:\n" +
          s"premises: ${oPremises.isDefined}\n" +
          s"address line 1: ${oAddressLine1.isDefined}\n" +
          s"address line 2: ${oAddressLine2.isDefined}\n" +
          s"locality: ${oLocality.isDefined}\n" +
          s"postcode: ${oPostalCode.isDefined}\n" +
          s"country: ${oCountry.isDefined}\n"
      }

      if (lines.length < 2) {
        JsError(incorrectAddressErrorMessage(s"Only ${lines.length} address lines returned from II for RO Address"))
      } else if (oPostalCode.isEmpty && oCountry.isEmpty) {
        JsError(incorrectAddressErrorMessage(s"Neither postcode nor country returned from II for RO Address"))
      } else {
        JsSuccess(Address(
          line1 = trimLine(lines.head, 27),
          line2 = trimLine(lines(1), 27),
          line3 = trimOptionalLine(lines.lift(2), 27),
          line4 = trimOptionalLine(lines.lift(3), 18),
          postCode = oPostalCode,
          country = if (oPostalCode.isEmpty) oCountry else None
        ))
      }
    }
  }

  val prePopReads: Reads[Address] = new Reads[Address] {
    override def reads(json: JsValue): JsResult[Address] = {
      val unvalidatedPostcode = json.\("postcode").asOpt[String](Formatters.normalizeTrimmedReads)
      val validatedPostcode = validatePostcode(unvalidatedPostcode).right.toOption

      val ctry = validatedPostcode match {
        case None => json.\("country").asOpt[String](Formatters.normalizeTrimmedReads)
        case Some(_) => None
      }

      if (validatedPostcode.isDefined || ctry.isDefined) {
        JsSuccess(Address(
          line1 = json.\("addressLine1").as[String],
          line2 = json.\("addressLine2").as[String],
          line3 = json.\("addressLine3").asOpt[String],
          line4 = json.\("addressLine4").asOpt[String],
          postCode = validatedPostcode,
          country = ctry,
          auditRef = json.\("auditRef").asOpt[String]
        ))
      } else {
        JsError("Neither country nor valid postcode defined in PrePop Address")
      }
    }
  }

  val prePopWrites: Writes[Address] = (
    (__ \ "addressLine1").write[String] and
      (__ \ "addressLine2").write[String] and
      (__ \ "addressLine3").writeNullable[String] and
      (__ \ "addressLine4").writeNullable[String] and
      (__ \ "postcode").writeNullable[String] and
      (__ \ "country").writeNullable[String] and
      (__ \ "auditRef").writeNullable[String]
    ) (unlift(Address.unapply))

  val prePopAddressesReads: Reads[Seq[Address]] = (__ \ "addresses").read[Seq[Address]](Reads.seq(prePopReads))

  val prePopFormat = Format(prePopReads, prePopWrites)
}
