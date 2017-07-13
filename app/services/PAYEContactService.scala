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

import javax.inject.{Inject, Singleton}

import audit.{CorrespondenceAddressAuditEvent, CorrespondenceAddressAuditEventDetail}
import connectors.{PAYERegistrationConnect, PAYERegistrationConnector}
import audit.{AmendedPAYEContactDetailsEvent, AmendedPAYEContactDetailsEventDetail, AuditPAYEContactDetails}
import config.{FrontendAuditConnector, FrontendAuthConnector}
import enums.CacheKeys
import models.Address
import models.view.{CompanyDetails => CompanyDetailsView, PAYEContact => PAYEContactView}
import models.api.{PAYEContact => PAYEContactAPI}
import enums.DownstreamOutcome
import models.external.{UserDetailsModel, UserIds}
import uk.gov.hmrc.play.audit.model.AuditEvent
import models.view.PAYEContactDetails
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYEContactService @Inject()(injPAYERegistrationConnector: PAYERegistrationConnector,
                                   injS4LService: S4LService,
                                   injCompanyDetailsService: CompanyDetailsService,
                                   injPrepopulationService: PrepopulationService) extends PAYEContactSrv {
  override val payeRegConnector = injPAYERegistrationConnector
  override val s4LService = injS4LService
  override val companyDetailsService = injCompanyDetailsService
  override val prepopService = injPrepopulationService
  override val authConnector = FrontendAuthConnector
  override val auditConnector = FrontendAuditConnector
}

trait PAYEContactSrv  {
  val payeRegConnector: PAYERegistrationConnect
  val s4LService: S4LSrv
  val companyDetailsService: CompanyDetailsSrv
  val prepopService: PrepopulationSrv
  val authConnector: AuthConnector
  val auditConnector: AuditConnector

  private[services] def viewToAPI(viewData: PAYEContactView): Either[PAYEContactView, PAYEContactAPI] = viewData match {
    case PAYEContactView(Some(contactDetails), Some(correspondenceAddress)) =>
      Right(PAYEContactAPI(contactDetails, correspondenceAddress))
    case _ => Left(viewData)
  }

  private[services] def apiToView(apiData: PAYEContactAPI): PAYEContactView = {
    PAYEContactView(Some(apiData.contactDetails), Some(apiData.correspondenceAddress))
  }

  private def saveToS4L(regId: String, viewData: PAYEContactView)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.saveForm[PAYEContactView](CacheKeys.PAYEContact.toString, viewData, regId).map(_ => viewData)
  }

  private[services] def convertOrCreatePAYEContactView(oAPI: Option[PAYEContactAPI])(implicit hc: HeaderCarrier): PAYEContactView = {
    oAPI match {
      case Some(detailsAPI) => apiToView(detailsAPI)
      case None => PAYEContactView(None, None)
    }
  }

  def getCorrespondenceAddresses(correspondenceAddress: Option[Address], companyDetails: CompanyDetailsView): Map[String, Address] = {
    correspondenceAddress map {
      case address@companyDetails.roAddress if companyDetails.ppobAddress.contains(companyDetails.roAddress) => Map("correspondence" -> address)
      case address@companyDetails.roAddress => Map("correspondence" -> address) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      case addr: Address if companyDetails.ppobAddress.contains(addr) => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr)
      case addr: Address => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
    } getOrElse {
      if( companyDetails.ppobAddress.contains(companyDetails.roAddress) ) {
        Map("ro" -> companyDetails.roAddress)
      } else {
        Map("ro" -> companyDetails.roAddress) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      }
    }
  }

  def getPAYEContact(regId: String)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.fetchAndGet[PAYEContactView](CacheKeys.PAYEContact.toString, regId) flatMap {
      case Some(contactDetails) => Future.successful(contactDetails)
      case None => for {
        oDetails <- payeRegConnector.getPAYEContact(regId)
      } yield convertOrCreatePAYEContactView(oDetails)
    }
  }

  def submitPAYEContact(viewModel: PAYEContactView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel).fold(
      incompleteView =>
        saveToS4L(regId, incompleteView) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          details   <- payeRegConnector.upsertPAYEContact(regId, completeAPI)
          clearData <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }

  def submitPayeContactDetails(regId: String, viewData: PAYEContactDetails)
                              (implicit hc: HeaderCarrier, authContext: AuthContext): Future[DownstreamOutcome.Value] = {
    for {
      cachedContactData <- getPAYEContact(regId)
      _                 <- auditPAYEContactDetails(regId, viewData, cachedContactData.contactDetails)
      submitted         <- submitPAYEContact(PAYEContactView(Some(viewData), cachedContactData.correspondenceAddress), regId)
    } yield submitted
  }

  def dataHasNotChanged(viewData: PAYEContactDetails, s4lData: Option[PAYEContactDetails]): Boolean = if(s4lData.isDefined) flattenData(viewData) != flattenData(s4lData.get) else true

  def auditPAYEContactDetails(regId: String, viewData: PAYEContactDetails, s4lData: Option[PAYEContactDetails])
                             (implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[AuditResult] = {
    if(!dataHasNotChanged(viewData, s4lData)) {
      for {
        ids               <- authConnector.getIds[UserIds](authContext)
        authId            <- authConnector.getUserDetails[JsObject](authContext)
        eventDetail       = AmendedPAYEContactDetailsEventDetail(
          externalUserId             = ids.externalId,
          authProviderId             = authId.\("authProviderId").as[String],
          journeyId                  = regId,
          previousPAYEContactDetails = convertPAYEContactViewToAudit(s4lData.get),
          newPAYEContactDetails      = convertPAYEContactViewToAudit(viewData)
        )
        auditResult       <- auditConnector.sendEvent(new AmendedPAYEContactDetailsEvent(eventDetail))
      } yield auditResult
    } else {
      Future.successful(AuditResult.Disabled)
    }
  }

  def submitCorrespondence(regId: String, correspondenceAddress: Address)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact(regId) flatMap {
      data => submitPAYEContact(PAYEContactView(data.contactDetails, Some(correspondenceAddress)), regId)
    }
  }

  def auditCorrespondenceAddress(regId: String, addressUsed: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[AuditEvent] = {
    for {
      userIds <- authConnector.getIds[UserIds](user)
      userDetails <- authConnector.getUserDetails[UserDetailsModel](user)
    } yield {
      val event = new CorrespondenceAddressAuditEvent(CorrespondenceAddressAuditEventDetail(userIds.externalId, userDetails.authProviderId, regId, addressUsed))
      auditConnector.sendEvent(event)

      event
    }
  }

  private[services] def flattenData(data: PAYEContactDetails) = data.copy(name = data.name.trim.replace(" ", ""), digitalContactDetails = data.digitalContactDetails.copy(
    email         = data.digitalContactDetails.email map(_.trim.replace(" ", "").toLowerCase),
    phoneNumber   = data.digitalContactDetails.phoneNumber map(_.trim.replace(" ", "").toLowerCase),
    mobileNumber  = data.digitalContactDetails.mobileNumber map(_.trim.replace(" ", "").toLowerCase))
  )

  private def convertPAYEContactViewToAudit(viewData: PAYEContactDetails) = AuditPAYEContactDetails(
    contactName   = viewData.name,
    email         = viewData.digitalContactDetails.email,
    mobileNumber  = viewData.digitalContactDetails.mobileNumber,
    phoneNumber   = viewData.digitalContactDetails.phoneNumber
  )
}

