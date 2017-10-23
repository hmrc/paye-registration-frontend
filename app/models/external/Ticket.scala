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

package models.external

import play.api.libs.json.Json

case class Ticket (name: String,
                   email: String,
                   subject: String,
                   message: String,
                   referrer: String,
                   javascriptEnabled: String,
                   userAgent: String,
                   authId: String,
                   areaOfTax: String,
                   sessionId: String,
                   service: String)

object Ticket {
  implicit val format = Json.format[Ticket]
}
