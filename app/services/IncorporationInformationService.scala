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

import common.exceptions.DownstreamExceptions.CompanyDetailsNotFoundException
import connectors._
import enums.{CacheKeys, DownstreamOutcome}
import models.api.Director
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import models.view.Directors
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncorporationInformationService @Inject()(injkeystoreConnector: KeystoreConnector,
                                                injCoHoAPIConnector: IncorporationInformationConnector) extends IncorporationInformationSrv {
  override val keystoreConnector = injkeystoreConnector
  override val coHoAPIConnector = injCoHoAPIConnector
}

trait IncorporationInformationSrv {

  val coHoAPIConnector : IncorporationInformationConnect
  val keystoreConnector : KeystoreConnect

  def fetchAndStoreCoHoCompanyDetails(regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      coHoResp <- coHoAPIConnector.getCoHoCompanyDetails(regId)
      outcome <- processCoHoResponse(coHoResp)
    } yield outcome
  }

  private def processCoHoResponse(resp: IncorpInfoResponse)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    resp match {
      case IncorpInfoSuccessResponse(companyDetails) =>
        keystoreConnector.cache[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString, companyDetails) map {
          cacheMap => DownstreamOutcome.Success
        }
      case _ => Future.successful(DownstreamOutcome.Failure)
    }
  }

  def getStoredCompanyName()(implicit hc: HeaderCarrier): Future[String] = {
    keystoreConnector.fetchAndGet[CoHoCompanyDetailsModel](CacheKeys.CoHoCompanyDetails.toString) map {
      case Some(model) => model.companyName
      case _ => throw new CompanyDetailsNotFoundException()
    }
  }

  def getDirectorDetails(txId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    for {
      officerList <- coHoAPIConnector.getOfficerList(txId)
      directorDetails <- convertOfficerList2Directors(officerList)
    } yield directorDetails
  }

  private def convertOfficerList2Directors(officerList: OfficerList): Future[Directors] = {
    val directors = officerList.items.collect {
      case officer: Officer if officer.role.contains("director") => Director(name = officer.name, nino = None)
    }

    Future.successful(Directors(directorMapping = (directors.indices.map(_.toString) zip directors).toMap))
  }

}
