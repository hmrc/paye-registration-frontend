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

package services

import javax.inject.Singleton

import audit._
import config.{FrontendAuditConnector, FrontendAuthConnector}
import models.DigitalContactDetails
import models.external.{UserDetailsModel, UserIds}
import models.view.PAYEContactDetails
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuditService extends AuditSrv {
  override val authConnector = FrontendAuthConnector
  override val auditConnector = FrontendAuditConnector
}

trait AuditSrv {
  val authConnector: AuthConnector
  val auditConnector: AuditConnector

  def auditBusinessContactDetails(regId: String, newData: DigitalContactDetails, previousData: DigitalContactDetails)
                                 (implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[AuditResult] = {
    for {
      ids         <- authConnector.getIds[UserIds](authContext)
      authId      <- authConnector.getUserDetails[UserDetailsModel](authContext)
      eventDetail = AmendedBusinessContactDetailsEventDetail(ids.externalId, authId.authProviderId, regId, previousData, newData)
      auditResult <- auditConnector.sendEvent(new AmendedBusinessContactDetailsEvent(eventDetail))
    } yield auditResult
  }

  def auditPPOBAddress(regId: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[AuditResult] = {
    for {
      userIds     <- authConnector.getIds[UserIds](user)
      userDetails <- authConnector.getUserDetails[UserDetailsModel](user)
      event       = new PPOBAddressAuditEvent(PPOBAddressAuditEventDetail(userIds.externalId, userDetails.authProviderId, regId))
      auditResult <- auditConnector.sendEvent(event)
    } yield auditResult
  }

  def auditPAYEContactDetails(regId: String, newData: PAYEContactDetails, previousData: Option[PAYEContactDetails])
                             (implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[AuditResult] = {

    def convertPAYEContactViewToAudit(viewData: PAYEContactDetails) = AuditPAYEContactDetails(
      contactName   = viewData.name,
      email         = viewData.digitalContactDetails.email,
      mobileNumber  = viewData.digitalContactDetails.mobileNumber,
      phoneNumber   = viewData.digitalContactDetails.phoneNumber
    )

    if( previousData.nonEmpty ) {
      for {
        ids <- authConnector.getIds[UserIds](authContext)
        authId <- authConnector.getUserDetails[JsObject](authContext)
        eventDetail = AmendedPAYEContactDetailsEventDetail(
          externalUserId = ids.externalId,
          authProviderId = authId.\("authProviderId").as[String],
          journeyId = regId,
          previousPAYEContactDetails = convertPAYEContactViewToAudit(previousData.get),
          newPAYEContactDetails = convertPAYEContactViewToAudit(newData)
        )
        auditResult <- auditConnector.sendEvent(new AmendedPAYEContactDetailsEvent(eventDetail))
      } yield auditResult
    } else {
      Future.successful(AuditResult.Disabled)
    }
  }

  def auditCorrespondenceAddress(regId: String, addressUsed: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[AuditResult] = {
    for {
      userIds <- authConnector.getIds[UserIds](user)
      userDetails <- authConnector.getUserDetails[UserDetailsModel](user)
      event = new CorrespondenceAddressAuditEvent(CorrespondenceAddressAuditEventDetail(userIds.externalId, userDetails.authProviderId, regId, addressUsed))
      auditResult <- auditConnector.sendEvent(event)
    } yield auditResult
  }
}
