/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{Address, DigitalContactDetails}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CompanyDetails(companyName: String,
                          tradingName: Option[TradingName],
                          roAddress: Address,
                          ppobAddress: Option[Address],
                          businessContactDetails: Option[DigitalContactDetails])

case class TradingName (differentName: Boolean,
                        tradingName:Option[String])

object TradingName {
  implicit val format = Json.format[TradingName]
}


object CompanyDetails {
  implicit val businessContactFormat = DigitalContactDetails.format
  implicit val format = Json.format[CompanyDetails]

  val tradingNameApiPrePopReads: Reads[Option[String]] = (__ \ "tradingName").readNullable[String]
  val tradingNameApiPrePopWrites: Writes[String] = new Writes[String] {
    override def writes(tradingName: String): JsValue = Json.obj("tradingName" -> tradingName)
  }
}