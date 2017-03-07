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

import play.api.libs.json.{Format, Json, Reads, Writes}

case class ChosenAddress (chosenAddress: AddressChoice.Value)

object AddressChoice extends Enumeration {
  val roAddress             = Value
  val ppobAddress           = Value
  val correspondenceAddress = Value
  val other                 = Value

  def fromString(choice: String): Value = choice match {
    case "roAddress"             => roAddress
    case "ppobAddress"           => ppobAddress
    case "correspondenceAddress" => correspondenceAddress
    case "other"                 => other
  }

  implicit val format = Format(Reads.enumNameReads(AddressChoice), Writes.enumNameWrites)
}

object ChosenAddress {
  implicit val format = Json.format[ChosenAddress]
}
