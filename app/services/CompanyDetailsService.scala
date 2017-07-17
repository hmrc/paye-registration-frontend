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

import audit._
import config.{FrontendAuditConnector, FrontendAuthConnector}
import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import models.external.{UserDetailsModel, UserIds}
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RegistrationWhitelist

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CompanyDetailsService @Inject()(injPAYERegistrationConnector: PAYERegistrationConnector,
                                      injCoHoAPIService: IncorporationInformationService,
                                      injS4LService: S4LService,
                                      injCompRegConnector : CompanyRegistrationConnector,
                                      injCohoAPIConnector: IncorporationInformationConnector,
                                      injPrepopulationService: PrepopulationService) extends CompanyDetailsSrv {

  override val payeRegConnector = injPAYERegistrationConnector
  override val compRegConnector = injCompRegConnector
  override val cohoAPIConnector = injCohoAPIConnector
  override val cohoService = injCoHoAPIService
  override val s4LService = injS4LService
  override val authConnector = FrontendAuthConnector
  override val auditConnector = FrontendAuditConnector
  val prepopService = injPrepopulationService
}

trait CompanyDetailsSrv extends RegistrationWhitelist {

  val payeRegConnector: PAYERegistrationConnect
  val compRegConnector: CompanyRegistrationConnect
  val cohoAPIConnector: IncorporationInformationConnect
  val cohoService: IncorporationInformationSrv
  val s4LService: S4LSrv
  val authConnector: AuthConnector
  val auditConnector: AuditConnector
  val prepopService: PrepopulationSrv

  def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    s4LService.fetchAndGet[CompanyDetailsView](CacheKeys.CompanyDetails.toString, regId) flatMap {
      case Some(companyDetails) => Future.successful(companyDetails)
      case None => for {
        oDetails <- ifRegIdNotWhitelisted(regId) {
          payeRegConnector.getCompanyDetails(regId)
        }
        details  <- convertOrCreateCompanyDetailsView(regId, oDetails)
        viewDetails <- saveToS4L(details, regId)
      } yield viewDetails
    }
  }

  private[services] def convertOrCreateCompanyDetailsView(regId: String, oAPI: Option[CompanyDetailsAPI])(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    oAPI match {
      case Some(detailsAPI) => Future.successful(apiToView(detailsAPI))
      case None => for {
        details <- cohoService.getStoredCompanyDetails
        oPrepopBCD <- prepopService.getBusinessContactDetails(regId)
      } yield CompanyDetailsView(details.companyName, None, details.roAddress, None, oPrepopBCD)
    }
  }

  private def saveToS4L(viewData: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    s4LService.saveForm[CompanyDetailsView](CacheKeys.CompanyDetails.toString, viewData, regId).map(_ => viewData)
  }

  private[services] def saveCompanyDetails(viewModel: CompanyDetailsView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel) fold(
      incompleteView =>
        saveToS4L(incompleteView, regId) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          details       <- payeRegConnector.upsertCompanyDetails(regId, completeAPI)
          clearData     <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
      )
  }

  def submitTradingName(tradingNameView: TradingNameView, regId: String, txId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails(regId, txId)
      outcome <- saveCompanyDetails(details.copy(tradingName = Some(tradingNameView)), regId)
    } yield outcome
  }

  def getPPOBPageAddresses(companyDetailsView: CompanyDetailsView): (Map[String, Address]) = {
    companyDetailsView.ppobAddress.map {
      case companyDetailsView.roAddress => Map("ppob" -> companyDetailsView.roAddress)
      case addr: Address => Map("ro" -> companyDetailsView.roAddress, "ppob" -> addr)
      }.getOrElse(Map("ro" -> companyDetailsView.roAddress))
  }

  def copyROAddrToPPOBAddr(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails(regId, txId)
      outcome <- saveCompanyDetails(details.copy(ppobAddress = Some(details.roAddress)), regId)
    } yield outcome
  }

  def submitPPOBAddr(ppobAddr: Address, regId: String, txId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails(regId, txId)
      outcome <- saveCompanyDetails(details.copy(ppobAddress = Some(ppobAddr)), regId)
    } yield outcome
  }

  def submitBusinessContact(businessContact: DigitalContactDetails, regId: String, txId: String)(implicit hc: HeaderCarrier, authContext: AuthContext): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails(regId, txId) flatMap {
        case currentDetails if dataHasChanged(businessContact, currentDetails.businessContactDetails) =>
          auditBusinessContactDetails(regId, businessContact, currentDetails.businessContactDetails.get) map {
            _ => currentDetails
          }
        case currentDetails => Future.successful(currentDetails)
      }
      outcome <- saveCompanyDetails(details.copy(businessContactDetails = Some(businessContact)), regId)
    } yield outcome
  }

  def auditBusinessContactDetails(regId: String, newData: DigitalContactDetails, previousData: DigitalContactDetails)
                             (implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[AuditResult] = {
    for {
      ids         <- authConnector.getIds[UserIds](authContext)
      authId      <- authConnector.getUserDetails[UserDetailsModel](authContext)
      eventDetail = AmendedBusinessContactDetailsEventDetail(ids.externalId, authId.authProviderId, regId, previousData, newData)
      auditResult <- auditConnector.sendEvent(new AmendedBusinessContactDetailsEvent(eventDetail))
    } yield auditResult
  }

  private[services] def dataHasChanged(viewData: DigitalContactDetails, s4lData: Option[DigitalContactDetails]): Boolean = s4lData.exists(flattenData(viewData) != flattenData(_))

  private[services] def flattenData(details: DigitalContactDetails): DigitalContactDetails = details.copy(
    email = details.email map(_.trim.replace(" ", "").toLowerCase),
    mobileNumber = details.mobileNumber map(_.trim.replace(" ", "").toLowerCase),
    phoneNumber = details.phoneNumber map(_.trim.replace(" ","").toLowerCase)
  )

  def auditPPOBAddress(regId: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[AuditResult] = {
    for {
      userIds     <- authConnector.getIds[UserIds](user)
      userDetails <- authConnector.getUserDetails[UserDetailsModel](user)
      event       = new PPOBAddressAuditEvent(PPOBAddressAuditEventDetail(userIds.externalId, userDetails.authProviderId, regId))
      auditResult <- auditConnector.sendEvent(event)
    } yield auditResult
  }

  private[services] def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
    val tradingNameView = apiModel.tradingName.map {
      tName => Some(TradingNameView(differentName = true, Some(tName)))
    }.getOrElse {
      Some(TradingNameView(differentName = false, None))
    }

    CompanyDetailsView(
      apiModel.companyName,
      tradingNameView,
      apiModel.roAddress,
      Some(apiModel.ppobAddress),
      Some(apiModel.businessContactDetails)
    )
  }

  private[services] def viewToAPI(viewData: CompanyDetailsView): Either[CompanyDetailsView, CompanyDetailsAPI] = viewData match {
    case CompanyDetailsView(companyName, Some(tradingName), roAddress, Some(ppobAddress), Some(businessContactDetails)) =>
      Right(CompanyDetailsAPI(companyName, tradingNameAPIValue(tradingName), roAddress, ppobAddress, businessContactDetails))
    case _ => Left(viewData)
  }

  private def tradingNameAPIValue(tradingNameView: TradingNameView): Option[String] = {
    if (tradingNameView.differentName) tradingNameView.tradingName else None
  }


}
