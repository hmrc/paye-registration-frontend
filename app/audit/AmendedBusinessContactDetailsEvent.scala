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


import play.api.libs.json.{Format, JsObject, Json, __}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.play.http.HeaderCarrier

case class AuditBusinessContactDetails(email: Option[String],
                                   mobileNumber: Option[String],
                                   phoneNumber: Option[String])

object AuditBusinessContactDetails {
  implicit val auditBusinessContactDetailsFormat: Format[AuditBusinessContactDetails] = (
      (__ \ "email").formatNullable[String] and
      (__ \ "mobileNumber").formatNullable[String] and
      (__ \ "phoneNumber").formatNullable[String]
    )(AuditBusinessContactDetails.apply, unlift(AuditBusinessContactDetails.unapply))
}

case class AmendedBusinessContactDetailsEventDetail(externalUserId: String,
                                              authProviderId: String,
                                              journeyId: String,
                                              previousContactDetails: AuditBusinessContactDetails,
                                              newContactDetails: AuditBusinessContactDetails)

object AmendedBusinessContactDetailsEventDetail {
  implicit val amendedBusinessContactDetailsEventFormat: Format[AmendedBusinessContactDetailsEventDetail] = (
    (__ \ "externalUserId").format[String] and
      (__ \ "authProviderId").format[String] and
      (__ \ "journeyId").format[String] and
      (__ \ "previousContactDetails").format[AuditBusinessContactDetails] and
      (__ \ "newContactDetails").format[AuditBusinessContactDetails]
    )(AmendedBusinessContactDetailsEventDetail.apply, unlift(AmendedBusinessContactDetailsEventDetail.unapply))
}

class AmendedBusinessContactDetailsEvent(detail: AmendedBusinessContactDetailsEventDetail)(implicit hc: HeaderCarrier)
  extends RegistrationAuditEvent("businessContactDetailsAmendment", None, Json.toJson(detail).as[JsObject])(hc)