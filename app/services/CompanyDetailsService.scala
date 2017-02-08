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

import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import models.view.{Address, CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CompanyDetailsService @Inject()(
                                       injKeystoreConnector: KeystoreConnector,
                                       injPAYERegistrationConnector: PAYERegistrationConnector,
                                       injCoHoAPIService: CoHoAPIService,
                                       injS4LService: S4LService
                                     )
  extends CompanyDetailsSrv {
  override val keystoreConnector = injKeystoreConnector
  override val payeRegConnector = injPAYERegistrationConnector
  override val cohoService = injCoHoAPIService
  override val s4LService = injS4LService
}

trait CompanyDetailsSrv extends CommonService {

  val payeRegConnector: PAYERegistrationConnect
  val cohoService: CoHoAPISrv
  val s4LService: S4LSrv

  def getCompanyDetails()(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    s4LService.fetchAndGet[CompanyDetailsView](CacheKeys.CompanyDetails.toString) flatMap {
      case Some(companyDetails) => Future.successful(companyDetails)
      case None => for {
        regID    <- fetchRegistrationID
        oDetails <- payeRegConnector.getCompanyDetails(regID)
        details  <- convertOrCreateCompanyDetailsView(oDetails)
        viewDetails <- saveToS4L(details)
      } yield viewDetails
    }
  }

  private[services] def convertOrCreateCompanyDetailsView(oAPI: Option[CompanyDetailsAPI])(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    oAPI match {
      case Some(detailsAPI) => Future.successful(apiToView(detailsAPI))
      case None => for {
        cName   <- cohoService.getStoredCompanyName
      } yield CompanyDetailsView(None, cName, None, None)
    }
  }

  private def saveToS4L(viewData: CompanyDetailsView)(implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    s4LService.saveForm[CompanyDetailsView](CacheKeys.CompanyDetails.toString, viewData).map(_ => viewData)
  }

  private[services] def saveCompanyDetails(viewModel: CompanyDetailsView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel) fold(
      incompleteView =>
        saveToS4L(incompleteView) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          regID     <- fetchRegistrationID
          details   <- payeRegConnector.upsertCompanyDetails(regID, completeAPI)
          clearData <- s4LService.clear
        } yield DownstreamOutcome.Success
      )
  }

  def submitTradingName(tradingNameView: TradingNameView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails
      outcome <- saveCompanyDetails(details.copy(tradingName = Some(tradingNameView)))
    } yield outcome
  }

  def getROAddress()(implicit hc: HeaderCarrier): Future[Address] = {
    for {
      regID   <- fetchRegistrationID
      details <- getCompanyDetails
      //TODO: Add connector call for undefined address when connector implemented
    } yield details.roAddress.get
  }

  def submitROAddress(address: Address)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      details <- getCompanyDetails
      outcome <- saveCompanyDetails(details.copy(roAddress = Some(address)))
    } yield outcome
  }

  private[services] def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
    val tradingNameView = apiModel.tradingName.map {
      tName => Some(TradingNameView(differentName = true, Some(tName)))
    }.getOrElse {
      Some(TradingNameView(differentName = false, None))
    }

    CompanyDetailsView(
      apiModel.crn,
      apiModel.companyName,
      tradingNameView,
      Some(apiModel.address)
    )
  }

  private[services] def viewToAPI(viewData: CompanyDetailsView): Either[CompanyDetailsView, CompanyDetailsAPI] = viewData match {
    case CompanyDetailsView(crn, companyName, Some(tradingName), Some(address)) =>
      Right(CompanyDetailsAPI(crn, companyName, tradingNameAPIValue(tradingName), address))
    case _ => Left(viewData)
  }

  private def tradingNameAPIValue(tradingNameView: TradingNameView): Option[String] = {
    if (tradingNameView.differentName) tradingNameView.tradingName else None
  }

}
