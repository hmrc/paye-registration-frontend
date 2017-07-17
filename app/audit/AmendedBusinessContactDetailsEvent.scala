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

package audit


import audit.RegistrationAuditEvent.{AUTH_PROVIDER_ID, EXTERNAL_USER_ID, JOURNEY_ID}
import models.DigitalContactDetails
import play.api.libs.json.{JsObject, JsValue, Json, Writes, __}
import uk.gov.hmrc.play.http.HeaderCarrier

case class AmendedBusinessContactDetailsEventDetail(externalUserId: String,
                                                    authProviderId: String,
                                                    regId: String,
                                                    previousContactDetails: DigitalContactDetails,
                                                    newContactDetails: DigitalContactDetails)

object AmendedBusinessContactDetailsEventDetail {
  private val PREVIOUS_CONTACT_DETAILS = "previousContactDetails"
  private val NEW_CONTACT_DETAILS = "newContactDetails"

  implicit val writes = new Writes[AmendedBusinessContactDetailsEventDetail] {
    override def writes(detail: AmendedBusinessContactDetailsEventDetail): JsValue = Json.obj(
      EXTERNAL_USER_ID -> detail.externalUserId,
      AUTH_PROVIDER_ID -> detail.authProviderId,
      JOURNEY_ID -> detail.regId,
      PREVIOUS_CONTACT_DETAILS -> detail.previousContactDetails,
      NEW_CONTACT_DETAILS -> detail.newContactDetails
    )
  }
}

class AmendedBusinessContactDetailsEvent(detail: AmendedBusinessContactDetailsEventDetail)(implicit hc: HeaderCarrier)
  extends RegistrationAuditEvent("businessContactAmendment", None, Json.toJson(detail).as[JsObject])(hc)