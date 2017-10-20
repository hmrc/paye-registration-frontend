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
import models.api.Director
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import models.view.Directors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

@Singleton
class IncorporationInformationService @Inject()(val keystoreConnector: KeystoreConnector,
                                                val incorpInfoConnector: IncorporationInformationConnector) extends IncorporationInformationSrv

trait IncorporationInformationSrv {

  val incorpInfoConnector : IncorporationInformationConnect
  val keystoreConnector : KeystoreConnect

  def getCompanyDetails(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[CoHoCompanyDetailsModel] = {
    incorpInfoConnector.getCoHoCompanyDetails(regId, txId) map {
      case IncorpInfoSuccessResponse(companyDetails) => companyDetails
      case IncorpInfoBadRequestResponse              => throw new BadRequestException(s"Received a BadRequest status code when expecting company details for regId: $regId / TX-ID: $txId")
      case IncorpInfoErrorResponse(ex)               => throw ex
    }
  }

  def getDirectorDetails(txId: String)(implicit hc: HeaderCarrier): Future[Directors] = {
    for {
      officerList     <- incorpInfoConnector.getOfficerList(txId)
      directorDetails <- convertOfficerList2Directors(officerList)
    } yield directorDetails
  }

  private def convertOfficerList2Directors(officerList: OfficerList): Future[Directors] = {
    val directors = officerList.items.collect {
      case officer: Officer if officer.resignedOn.isEmpty && officer.role.equals("director") => Director(name = officer.name, nino = None)
    }
    Future.successful(Directors(directorMapping = (directors.indices.map(_.toString) zip directors).toMap))
  }

}
