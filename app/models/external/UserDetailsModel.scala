/*
 * Copyright 2023 HM Revenue & Customs
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

case class UserDetailsModel(name: String,
                            email: String,
                            affinityGroup: String,
                            description: Option[String] = None,
                            lastName: Option[String] = None,
                            dateOfBirth: Option[String] = None,
                            postcode: Option[String] = None,
                            authProviderId: String,
                            authProviderType: String)

object UserDetailsModel {
  implicit val format = Json.format[UserDetailsModel]
}

case class UserIds(internalId: String,
                   externalId: String)

object UserIds {
  implicit val format = Json.format[UserIds]
}

case class AuditingInformation(externalId: String,
                               providerId: String)
