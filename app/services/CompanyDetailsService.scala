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

import connectors._
import enums.DownstreamOutcome
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import common.exceptions.DownstreamExceptions.PAYEMicroserviceException
import common.exceptions.InternalExceptions.APIConversionException
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CompanyDetailsService extends CompanyDetailsService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegConnector = PAYERegistrationConnector
  override val cohoService = CoHoAPIService
  //$COVERAGE-ON$
}

trait CompanyDetailsService extends CommonService {

  val payeRegConnector: PAYERegistrationConnector
  val cohoService: CoHoAPIService

  def getCompanyDetails()(implicit hc: HeaderCarrier): Future[Option[CompanyDetailsView]] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegConnector.getCompanyDetails(regID)
    } yield regResponse map {
      details => apiToView(details)
    }
  }

  def getCompanyName(detailsViewOption: Option[CompanyDetailsView])(implicit hc: HeaderCarrier): Future[String] = {
    detailsViewOption.map {
      detailsView => Future.successful(detailsView.companyName)
    }.getOrElse {
      cohoService.getStoredCompanyName()
    }
  }

  def submitTradingName(tradingNameView: TradingNameView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID <- fetchRegistrationID
      oCompanyDetails <- getCompanyDetails
      updatedDetails <- addTradingNameToCompanyDetails(tradingNameView, oCompanyDetails)
      outcome <- submitCompanyDetails(updatedDetails, regID)
    } yield outcome
  }

  private[services] def addTradingNameToCompanyDetails(tradingNameView: TradingNameView, oCompanyDetails: Option[CompanyDetailsView])
                                                      (implicit hc: HeaderCarrier): Future[CompanyDetailsView] = {
    oCompanyDetails match {
      case Some(companyDetails) => Future.successful(companyDetails.copy(tradingName = Some(tradingNameView)))
      case None => cohoService.getStoredCompanyName() map {
        cName => CompanyDetailsView(None, cName, Some(tradingNameView))
      }
    }
  }

  private[services] def submitCompanyDetails(detailsView: CompanyDetailsView, regID:String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    payeRegConnector.upsertCompanyDetails(regID, viewToAPI(detailsView)) map {
      _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }
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
      tradingNameView
    )
  }

  private[services] def viewToAPI(viewModel: CompanyDetailsView): CompanyDetailsAPI = {
    val tradingNameAPI: Option[String] = viewModel.tradingName.map {
      companyDetails => companyDetails.differentName match {
        case true  => companyDetails.tradingName
        case false => None
      }
    }.getOrElse {
      Logger.warn("[CompanyDetailsService] [viewToAPI] Unable to create API model from view model - missing TradingNameView model")
      throw new APIConversionException("Trading name view not defined when converting Company Details View to API model")
    }

    CompanyDetailsAPI(
      viewModel.crn,
      viewModel.companyName,
      tradingNameAPI
    )
  }

}
