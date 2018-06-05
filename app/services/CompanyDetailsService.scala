/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject
import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import models.external.AuditingInformation
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.{Address, DigitalContactDetails}
import play.api.data.Form
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.RegistrationWhitelist

import scala.concurrent.Future

class CompanyDetailsServiceImpl @Inject()(val payeRegConnector: PAYERegistrationConnector,
                                          val incorpInfoService: IncorporationInformationService,
                                          val s4LService: S4LService,
                                          val compRegConnector : CompanyRegistrationConnector,
                                          val prepopService: PrepopulationService,
                                          val auditService: AuditService) extends CompanyDetailsService

trait CompanyDetailsService extends RegistrationWhitelist {

  val payeRegConnector: PAYERegistrationConnector
  val compRegConnector: CompanyRegistrationConnector
  val incorpInfoService: IncorporationInformationService
  val s4LService: S4LService
  val prepopService: PrepopulationService
  val auditService: AuditService

  def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    s4LService.fetchAndGet[CompanyDetailsView](CacheKeys.CompanyDetails.toString, regId) flatMap {
      case Some(companyDetails) => Future.successful(companyDetails)
      case None => for {
        oDetails    <- ifRegIdNotWhitelisted(regId) {
          payeRegConnector.getCompanyDetails(regId)
        }
        details     <- convertOrCreateCompanyDetailsView(regId, txId, oDetails)
        viewDetails <- saveToS4L(details, regId)
      } yield viewDetails
    }
  }
  def withLatestCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    for {
      oCoHoCompanyDetails <- incorpInfoService.getCompanyDetails(regId, txId) map(Some(_)) recover {case _ => None}
      companyDetails      <- getCompanyDetails(regId, txId)
      details             =  oCoHoCompanyDetails.map(ch => companyDetails.copy(companyName = ch.companyName, roAddress = ch.roAddress)).getOrElse(companyDetails)
      _                   <- s4LService.saveForm[CompanyDetailsView](CacheKeys.CompanyDetails.toString, details, regId)
    } yield details
  }

  def getTradingNamePrepop(regId: String, tradingName: Option[TradingNameView])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    if(tradingName.exists(_.differentName)) Future.successful(None) else prepopService.getTradingName(regId)
  }

  private[services] def convertOrCreateCompanyDetailsView(regId: String, txId: String, oAPI: Option[CompanyDetailsAPI])(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    oAPI match {
      case Some(detailsAPI) => Future.successful(apiToView(detailsAPI))
      case None => for {
        details     <- incorpInfoService.getCompanyDetails(regId, txId)
        oPrepopBCD  <- prepopService.getBusinessContactDetails(regId)
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
    getCompanyDetails(regId, txId).flatMap { details =>
      saveCompanyDetails(details.copy(tradingName = Some(tradingNameView)), regId).flatMap { outcome =>
        tradingNameView.tradingName.fold(Future.successful(outcome)) { tName =>
          prepopService.saveTradingName(regId, tName).map(_ => outcome)
        }
      }
    }
  }

  def getPPOBPageAddresses(companyDetailsView: CompanyDetailsView): (Map[String, Address]) = {
    companyDetailsView.ppobAddress.map {
      case companyDetailsView.roAddress => Map("ppob" -> companyDetailsView.roAddress)
      case addr: Address                => Map("ro" -> companyDetailsView.roAddress, "ppob" -> addr)
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

  def submitBusinessContact(businessContact: DigitalContactDetails, regId: String, txId: String)
                           (implicit hc: HeaderCarrier, auditInfo: AuditingInformation, req:Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails(regId, txId) flatMap {
        case currentDetails if dataHasChanged(businessContact, currentDetails.businessContactDetails) =>
          auditService.auditBusinessContactDetails(regId, businessContact, currentDetails.businessContactDetails.get) map {
            _ => currentDetails
          }
        case currentDetails => Future.successful(currentDetails)
      }
      outcome <- saveCompanyDetails(details.copy(businessContactDetails = Some(businessContact)), regId)
    } yield outcome
  }

  private[services] def dataHasChanged(viewData: DigitalContactDetails, s4lData: Option[DigitalContactDetails]): Boolean = s4lData.exists(flattenData(viewData) != flattenData(_))

  private[services] def flattenData(details: DigitalContactDetails): DigitalContactDetails = details.copy(
    email         = details.email map(_.trim.replace(" ", "").toLowerCase),
    mobileNumber  = details.mobileNumber map(_.trim.replace(" ", "").toLowerCase),
    phoneNumber   = details.phoneNumber map(_.trim.replace(" ","").toLowerCase)
  )

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