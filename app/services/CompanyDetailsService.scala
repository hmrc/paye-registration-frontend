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
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import common.exceptions.DownstreamExceptions.PAYEMicroserviceException
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CompanyDetailsService extends CompanyDetailsService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegConnector = PAYERegistrationConnector
  //$COVERAGE-ON$
}

trait CompanyDetailsService extends CommonService {

  val payeRegConnector: PAYERegistrationConnector

  def getTradingName()(implicit hc: HeaderCarrier): Future[Option[TradingNameView]] = {
    getCompanyDetails() map {
      case Some(detailsView) => detailsView.tradingName
      case None => None
    }
  }

  private def getCompanyDetails()(implicit hc: HeaderCarrier): Future[Option[CompanyDetailsView]] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegConnector.getCompanyDetails(regID)
    } yield regResponse match {
      case PAYERegistrationSuccessResponse(details: CompanyDetailsAPI) => Some(apiToView(details))
      case PAYERegistrationNotFoundResponse => None
      case PAYERegistrationErrorResponse(e) => throw new PAYEMicroserviceException(e.getMessage)
      case resp => throw new PAYEMicroserviceException(s"Received $resp response from connector when expecting PAYERegSuccessResponse[Option[CompanyDetailsAPI]]")
    }
  }

  private def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
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

}
