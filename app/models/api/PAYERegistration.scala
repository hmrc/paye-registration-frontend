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

package models.api

import play.api.libs.json.Json

case class PAYERegistration (registrationID: String,
                             formCreationTimestamp: String,
                             completionCapacity: String,
                             companyDetails: CompanyDetails,
                             employment: Employment,
                             sicCodes: List[SICCode],
                             directors: List[Director],
                             payeContact: PAYEContact)

object PAYERegistration {
  implicit val companyDetailsFormat = CompanyDetails.format
  implicit val EmploymentFormat = Employment.formatModel
  implicit val DirectorFormat = Director.format
  implicit val contactFormat = PAYEContact.format
  implicit val format = Json.format[PAYERegistration]
}
