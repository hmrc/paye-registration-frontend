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

package services

import audit.{RegistrationAuditEventConstants, _}
import models.DigitalContactDetails
import models.external.AuditingInformation
import models.view.PAYEContactDetails
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditServiceImpl @Inject()(val auditConnector: AuditConnector) extends AuditService {
}

trait AuditService {
  val auditConnector: AuditConnector

  private[services] def now() = Instant.now()
  private[services] def eventId() = UUID.randomUUID().toString

  def sendEvent[T](auditType: String, detail: T, transactionName: Option[String] = None)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext, fmt: Writes[T]): Future[AuditResult] = {

    val event = ExtendedDataEvent(
      auditSource = auditConnector.auditingConfig.auditSource,
      auditType   = auditType,
      eventId     = eventId(),
      tags        = hc.toAuditTags(
        transactionName = transactionName.getOrElse(auditType),
        path = hc.otherHeaders.collectFirst { case (RegistrationAuditEventConstants.PATH, value) => value }.getOrElse("-")
      ),
      detail      = Json.toJson(detail),
      generatedAt = now()
    )

    auditConnector.sendExtendedEvent(event)
  }

  def auditBusinessContactDetails(regId: String, newData: DigitalContactDetails, previousData: DigitalContactDetails)
                                 (implicit auditInfo: AuditingInformation, headerCarrier: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] =
    sendEvent(
      auditType = "businessContactAmendment",
      detail = AmendedBusinessContactDetailsEventDetail(auditInfo.externalId, auditInfo.providerId, regId, previousData, newData)
    )

  def auditPPOBAddress(regId: String)
                      (implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] =
    sendEvent(
      auditType = "registeredOfficeUsedAsPrincipalPlaceOfBusiness",
      detail = PPOBAddressAuditEventDetail(auditInfo.externalId, auditInfo.providerId, regId)
    )

  def auditPAYEContactDetails(regId: String, newData: PAYEContactDetails, previousData: Option[PAYEContactDetails])
                             (implicit auditInfo: AuditingInformation, headerCarrier: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] = {

    def convertPAYEContactViewToAudit(viewData: PAYEContactDetails) = AuditPAYEContactDetails(
      contactName = viewData.name,
      email = viewData.digitalContactDetails.email,
      mobileNumber = viewData.digitalContactDetails.mobileNumber,
      phoneNumber = viewData.digitalContactDetails.phoneNumber
    )

    if (previousData.nonEmpty) {
      sendEvent("payeContactDetailsAmendment", AmendedPAYEContactDetailsEventDetail(
        externalUserId = auditInfo.externalId,
        authProviderId = auditInfo.providerId,
        journeyId = regId,
        previousPAYEContactDetails = convertPAYEContactViewToAudit(previousData.get),
        newPAYEContactDetails = convertPAYEContactViewToAudit(newData)
      ))
    } else {
      Future.successful(AuditResult.Disabled)
    }
  }

  def auditCorrespondenceAddress(regId: String, addressUsed: String)
                                (implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] =
    sendEvent(
      "correspondenceAddress",
      CorrespondenceAddressAuditEventDetail(auditInfo.externalId, auditInfo.providerId, regId, addressUsed)
    )
}
