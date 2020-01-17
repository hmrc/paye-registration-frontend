/*
 * Copyright 2020 HM Revenue & Customs
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

package models.api

import enums.PAYEStatus
import play.api.libs.functional.syntax._
import play.api.libs.json.{OFormat, __}

case class PAYERegistration(registrationID: String,
                            transactionID: String,
                            formCreationTimestamp: String,
                            status: PAYEStatus.Value,
                            completionCapacity: String,
                            companyDetails: CompanyDetails,
                            employmentInfo: Employment,
                            sicCodes: List[SICCode],
                            directors: List[Director],
                            payeContact: PAYEContact)

object PAYERegistration {
  implicit val format: OFormat[PAYERegistration] = (
    (__ \ "registrationID").format[String] and
    (__ \ "transactionID").format[String] and
    (__ \ "formCreationTimestamp").format[String] and
    (__ \ "status").format[PAYEStatus.Value] and
    (__ \ "completionCapacity").format[String] and
    (__ \ "companyDetails").format[CompanyDetails] and
    (__ \ "employmentInfo").format[Employment] and
    (__ \ "sicCodes").format[List[SICCode]] and
    (__ \ "directors").format[List[Director]] and
    (__ \ "payeContact").format[PAYEContact]
  )(PAYERegistration.apply, unlift(PAYERegistration.unapply))
}
