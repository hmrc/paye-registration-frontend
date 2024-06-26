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

package audit

import audit.RegistrationAuditEventConstants.{AUTH_PROVIDER_ID, EXTERNAL_USER_ID, JOURNEY_ID}
import play.api.libs.json.{Json, Writes}

case class CorrespondenceAddressAuditEventDetail(externalUserId: String,
                                                 authProviderId: String,
                                                 regId: String,
                                                 addressUsed: String)

object CorrespondenceAddressAuditEventDetail {
  private val ADDRESS_USED = "addressUsed"

  implicit val writes: Writes[CorrespondenceAddressAuditEventDetail] = Writes[CorrespondenceAddressAuditEventDetail] { detail =>
    Json.obj(
      EXTERNAL_USER_ID -> detail.externalUserId,
      AUTH_PROVIDER_ID -> detail.authProviderId,
      JOURNEY_ID -> detail.regId,
      ADDRESS_USED -> detail.addressUsed
    )
  }
}
