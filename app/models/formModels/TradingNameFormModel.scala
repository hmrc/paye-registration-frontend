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

package models.formModels

import models.payeRegistration.companyDetails.TradingName
import enums.YesNo
import play.api.libs.json.Json


case class TradingNameFormModel (tradeUnderDifferentName: String,
                             tradingName:Option[String]) {

  def this(data: TradingName) = {
    this (YesNo.fromBoolean(data.tradingName.isDefined).toString.toLowerCase, data.tradingName)
  }

  def toData: TradingName = {
    YesNo.fromString(this.tradeUnderDifferentName) match {
      case YesNo.Yes => TradingName(this.tradingName)
      case YesNo.No  => TradingName(None)

    }
  }
}

object TradingNameFormModel {
  implicit val formats = Json.format[TradingNameFormModel]
}
