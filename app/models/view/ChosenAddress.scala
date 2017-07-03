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

import play.api.Logger
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}

class ConvertToPrepopAddressException(msg: String) extends Exception(msg)

sealed trait AddressChoice
object AddressChoice {
  val prepopRegex = s"${PrepopAddress.prefix}[0-9]+"

  def fromString(choice: String): AddressChoice = choice match {
    case "roAddress"                           => ROAddress
    case "ppobAddress"                         => PPOBAddress
    case "correspondenceAddress"               => CorrespondenceAddress
    case "other"                               => Other
    case prepop if choice.matches(prepopRegex) => PrepopAddress.fromString(prepop)
    case _                                     => throw new ConvertToPrepopAddressException(s"Incorrect AddressChoice value $choice is not valid")
  }

  val reads = new Reads[AddressChoice] {
    override def reads(json: JsValue): JsResult[AddressChoice] = {
      try {
        JsSuccess(fromString(json.as[String]))
      } catch {
        case e: ConvertToPrepopAddressException =>
          Logger.warn(e.getMessage)
          JsError(e.getMessage)
      }
    }
  }

  val writes = new Writes[AddressChoice] {
    override def writes(addressChoice: AddressChoice): JsValue = {
      addressChoice match {
        case ROAddress             => JsString("roAddress")
        case PPOBAddress           => JsString("ppobAddress")
        case CorrespondenceAddress => JsString("correspondenceAddress")
        case Other                 => JsString("other")
        case prepop: PrepopAddress => Json.toJson[PrepopAddress](prepop)(PrepopAddress.writes)
      }
    }
  }

  implicit val format = Format(reads, writes)
}

case object ROAddress extends AddressChoice
case object PPOBAddress extends AddressChoice
case object CorrespondenceAddress extends AddressChoice
case object Other extends AddressChoice

case class PrepopAddress(index: Int) extends AddressChoice
object PrepopAddress {
  val prefix = "prepopAddress"

  def fromString(choice: String): PrepopAddress = {
    if( choice.startsWith(prefix) ) {
      try {
        val index = choice.substring(prefix.length).toInt
        PrepopAddress(index)
      } catch {
        case e: Exception =>
          val errMsg = s"[PrepopAddress] [fromString] Could not convert from String to PrepopAddress for value $choice not valid, error: ${e.getMessage}"
          Logger.warn(errMsg)
          throw new ConvertToPrepopAddressException(errMsg)
      }
    } else {
      val errMsg = s"[PrepopAddress] [fromString] Could not convert from String to PrepopAddress for value $choice not valid"
      Logger.warn(errMsg)
      throw new ConvertToPrepopAddressException(errMsg)
    }
  }

  val reads = new Reads[PrepopAddress] {
    override def reads(json: JsValue): JsResult[PrepopAddress] = {
      try {
        JsSuccess(fromString(json.as[String]))
      } catch {
        case e: ConvertToPrepopAddressException =>
          Logger.warn(e.getMessage)
          JsError(e.getMessage)
      }
    }
  }

  val writes = new Writes[PrepopAddress] {
    override def writes(prepopAddress: PrepopAddress): JsValue = JsString(s"$prefix${prepopAddress.index}")
  }

  implicit val format = Format(reads, writes)
}

case class ChosenAddress (chosenAddress: AddressChoice)
object ChosenAddress {
  implicit val format = Json.format[ChosenAddress]
}
