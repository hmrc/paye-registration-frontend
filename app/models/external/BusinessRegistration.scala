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

import common.exceptions
import enums.IncorporationStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BusinessRegistrationRequest(language: String)

object BusinessRegistrationRequest {
  implicit val formats: OFormat[BusinessRegistrationRequest] = Json.format[BusinessRegistrationRequest]
}

case class BusinessProfile(registrationID: String,
                           language: String)

object BusinessProfile {
  implicit val formats: OFormat[BusinessProfile] = Json.format[BusinessProfile]
}

case class CompanyRegistrationProfile(status: String,
                                      transactionId: String,
                                      ackRefStatus: Option[String] = None,
                                      paidIncorporation: Option[String] = None)

object CompanyRegistrationProfile {
  implicit val formats: OFormat[CompanyRegistrationProfile] = Json.format[CompanyRegistrationProfile]

  val companyRegistrationReads: Reads[CompanyRegistrationProfile] = (
    (__ \ "status").read[String] and
      (__ \ "confirmationReferences" \ "transaction-id").readNullable[String].map {
        case Some(txId) => txId
        case None => throw new exceptions.DownstreamExceptions.ConfirmationRefsNotFoundException
      } and
      (__ \ "acknowledgementReferences" \ "status").readNullable[String] and
      (__ \ "confirmationReferences" \ "payment-reference").readNullable[String]
    )(CompanyRegistrationProfile.apply _)
}

case class CurrentProfile(registrationID: String,
                          companyTaxRegistration: CompanyRegistrationProfile,
                          language: String,
                          payeRegistrationSubmitted: Boolean,
                          incorpStatus: Option[IncorporationStatus.Value])

object CurrentProfile {

  val reads: Reads[CurrentProfile] = (
    (__ \ "registrationID").read[String] and
      (__ \ "companyTaxRegistration").read[CompanyRegistrationProfile] and
      (__ \ "language").read[String] and
      ((__ \ "payeRegistrationSubmitted").read[Boolean] or Reads.pure(false)) and
      (__ \ "incorpStatus").readNullable[IncorporationStatus.Value]
    ) (CurrentProfile.apply _)

  val writes: OWrites[CurrentProfile] = Json.writes[CurrentProfile]

  implicit val format: Format[CurrentProfile] = Format(reads, writes)
}
