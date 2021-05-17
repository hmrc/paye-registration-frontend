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

package services

import audit._
import javax.inject.{Inject, Singleton}
import models.DigitalContactDetails
import models.external.AuditingInformation
import models.view.PAYEContactDetails
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditServiceImpl @Inject()(val auditConnector: AuditConnector)(implicit ec: ExecutionContext) extends AuditService {
}

trait AuditService {
  val auditConnector: AuditConnector

  def auditBusinessContactDetails(regId: String, newData: DigitalContactDetails, previousData: DigitalContactDetails)
                                 (implicit auditInfo: AuditingInformation, headerCarrier: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(new AmendedBusinessContactDetailsEvent(
      AmendedBusinessContactDetailsEventDetail(auditInfo.externalId, auditInfo.providerId, regId, previousData, newData)
    ))
  }

  def auditPPOBAddress(regId: String)(implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(
      new PPOBAddressAuditEvent(PPOBAddressAuditEventDetail(auditInfo.externalId, auditInfo.providerId, regId))
    )
  }

  def auditPAYEContactDetails(regId: String, newData: PAYEContactDetails, previousData: Option[PAYEContactDetails])
                             (implicit auditInfo: AuditingInformation, headerCarrier: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] = {

    def convertPAYEContactViewToAudit(viewData: PAYEContactDetails) = AuditPAYEContactDetails(
      contactName = viewData.name,
      email = viewData.digitalContactDetails.email,
      mobileNumber = viewData.digitalContactDetails.mobileNumber,
      phoneNumber = viewData.digitalContactDetails.phoneNumber
    )

    if (previousData.nonEmpty) {
      val eventDetail = AmendedPAYEContactDetailsEventDetail(
        externalUserId = auditInfo.externalId,
        authProviderId = auditInfo.providerId,
        journeyId = regId,
        previousPAYEContactDetails = convertPAYEContactViewToAudit(previousData.get),
        newPAYEContactDetails = convertPAYEContactViewToAudit(newData)
      )
      auditConnector.sendExtendedEvent(new AmendedPAYEContactDetailsEvent(eventDetail))
    } else {
      Future.successful(AuditResult.Disabled)
    }
  }

  def auditCorrespondenceAddress(regId: String, addressUsed: String)(implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent], ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(
      new CorrespondenceAddressAuditEvent(
        CorrespondenceAddressAuditEventDetail(auditInfo.externalId, auditInfo.providerId, regId, addressUsed)
      )
    )
  }
}
