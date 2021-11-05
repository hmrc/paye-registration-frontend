/*
 * Copyright 2021 HM Revenue & Customs
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

import models.api.Name
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads.DefaultJodaDateTimeReads
import play.api.libs.json._

case class Officer(name: Name,
                   role: String,
                   resignedOn: Option[DateTime],
                   appointmentLink: Option[String]) // custom read to pick up (if required - TBC)

object Officer {
  implicit val formatModel: Reads[Officer] = (
    (__ \ "name_elements").read[Name](Name.normalizeNameReads) and
      (__ \ "officer_role").read[String] and
      (__ \ "resigned_on").readNullable[DateTime] and
      (__ \ "appointment_link").readNullable[String]
    ) (Officer.apply _)

  val seqReads: Reads[Seq[Officer]] = new Reads[Seq[Officer]] {
    override def reads(json: JsValue): JsResult[Seq[Officer]] = Json.fromJson[Seq[Officer]](json)
  }
}

case class OfficerList(items: Seq[Officer])

object OfficerList {
  implicit val formatModel: Reads[OfficerList] = Officer.seqReads map OfficerList.apply
}
