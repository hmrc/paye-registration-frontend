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

package models.payeRegistration.companyDetails

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class CompanyDetails(
                           crn: Option[String],
                           companyName: String,
                           tradingName: Option[TradingName]
                           )

case class TradingName(tradingName: Option[String])

object TradingName {
  implicit val formats = Json.format[TradingName]
}

object CompanyDetails {
  implicit val locationFormat: Format[CompanyDetails] = (
    (JsPath \ "crn").formatNullable[String] and
    (JsPath \ "companyName").format[String] and
    (JsPath \ "tradingName").formatNullable[String]
      .inmap(str => str.map(tName => TradingName(Some(tName))), (tNameOpt: Option[TradingName]) => tNameOpt.map{tName => tName.tradingName}.getOrElse(None))
  )(CompanyDetails.apply, unlift(CompanyDetails.unapply))
}

