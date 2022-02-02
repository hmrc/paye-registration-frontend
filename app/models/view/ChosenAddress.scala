/*
 * Copyright 2022 HM Revenue & Customs
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

import common.Logging

class ConvertToPrepopAddressException(msg: String) extends Exception(msg)

sealed trait AddressChoice

object AddressChoice {
  def fromString(s: String): AddressChoice = s match {
    case "roAddress" => ROAddress
    case "ppobAddress" => PPOBAddress
    case "correspondenceAddress" => CorrespondenceAddress
    case "other" => Other
    case prepop => PrepopAddress.fromString(prepop)
  }
}

case object ROAddress extends AddressChoice {
  override def toString: String = "roAddress"
}

case object PPOBAddress extends AddressChoice {
  override def toString: String = "ppobAddress"
}

case object CorrespondenceAddress extends AddressChoice {
  override def toString: String = "correspondenceAddress"
}

case object Other extends AddressChoice {
  override def toString: String = "other"
}

case class PrepopAddress(index: Int) extends AddressChoice {
  override def toString: String = s"${PrepopAddress.prefix}$index"
}

object PrepopAddress extends Logging {
  val prefix = "prepopAddress"
  val prepopRegex = s"${PrepopAddress.prefix}[0-9]+"

  def fromString(s: String): PrepopAddress = {
    if (s.matches(prepopRegex)) {
      val index = s.substring(prefix.length).toInt
      PrepopAddress(index)
    } else {
      val errMsg = s"[PrepopAddress] [fromString] Could not convert from String to PrepopAddress for value $s not valid"
      logger.warn(errMsg)
      throw new ConvertToPrepopAddressException(errMsg)
    }
  }
}

case class ChosenAddress(chosenAddress: AddressChoice)
