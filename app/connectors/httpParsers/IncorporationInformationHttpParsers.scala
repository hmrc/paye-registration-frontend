/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.httpParsers

import common.exceptions
import common.exceptions.DownstreamExceptions
import connectors._
import enums.IncorporationStatus
import models.external.{CoHoCompanyDetailsModel, IncorpUpdateResponse, OfficerList}
import play.api.http.Status._
import play.api.libs.json.__
import play.api.mvc.Request
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

trait IncorporationInformationHttpParsers extends BaseHttpReads {
  _: BaseConnector =>

  override def unexpectedStatusException(functionName: String, url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new exceptions.DownstreamExceptions.IncorporationInformationResponseException(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  def setupSubscriptionHttpReads(regId: String, transactionId: String, subscriber: String, regime: String)
                                (implicit request: Request[_]): HttpReads[Option[IncorporationStatus.Value]] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK =>
        implicit val reads = IncorpUpdateResponse.reads(transactionId, subscriber, regime)
        Some(jsonParse(response)("setupSubscriptionHttpReads", Some(regId), Some(transactionId)))
      case ACCEPTED =>
        None
      case status =>
        unexpectedStatusHandling()("setupSubscription", url, status, Some(regId), Some(transactionId))
    }

  def cancelSubscriptionHttpReads(regId: String, transactionId: String)(implicit request: Request[_]): HttpReads[Boolean] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => true
      case NOT_FOUND =>
        infoLog(s"[cancelSubscriptionHttpReads] no subscription found when trying to delete subscription. it might already have been deleted for regId: $regId and txId: $transactionId")
        true
      case status =>
        unexpectedStatusHandling(Some(false))("cancelSubscriptionHttpReads", url, status, Some(regId), Some(transactionId))
    }

  def getCoHoCompanyDetailsHttpReads(regId: String, transactionId: String)(implicit request: Request[_]): HttpReads[IncorpInfoResponse] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK =>
        implicit val reads = CoHoCompanyDetailsModel.incorpInfoReads
        val cohoDetails = jsonParse(response)("getCoHoCompanyDetailsHttpReads", Some(regId), Some(transactionId))
        IncorpInfoSuccessResponse(cohoDetails)
      case BAD_REQUEST =>
        errorLog(s"[getCoHoCompanyDetailsHttpReads] Received a BadRequest status code when expecting company details for regId: $regId and txId: $transactionId")
        IncorpInfoBadRequestResponse
      case NOT_FOUND =>
        errorLog(s"[getCoHoCompanyDetailsHttpReads] Received a NotFound status code when expecting company details for regId: $regId and txId: $transactionId")
        IncorpInfoNotFoundResponse
      case status =>
        unexpectedStatusHandling()("getCoHoCompanyDetailsHttpReads", url, status, Some(regId), Some(transactionId))
    }

  def getIncorpInfoDateHttpReads(regId: String, transactionId: String)(implicit request: Request[_]): HttpReads[Option[LocalDate]] = (_: String, url: String, response: HttpResponse) => response.status match {
    case OK =>
      (response.json \ "incorporationDate").asOpt[String] match {
        case Some(potentialDate) => Try(LocalDate.parse(potentialDate)) match {
          case Success(date) => Some(date)
          case Failure(_) =>
            errorLog(s"[getIncorpInfoDateHttpReads] IncorpDate was retrieved from II but was not able to be parsed to LocalDate format. Value received: '$potentialDate' for regId: $regId and txId: $transactionId")
            None
        }
        case None =>
          infoLog(s"[getIncorpInfoDateHttpReads] No IncorpDate was retrieved from II for regId: $regId and txId: $transactionId")
          None
      }
    case NO_CONTENT =>
      None
    case status =>
      unexpectedStatusHandling()("getIncorpInfoDateHttpReads", url, status, Some(regId), Some(transactionId))
  }

  def getOfficersHttpReads(regId: String, transactionId: String)(implicit request: Request[_]): HttpReads[OfficerList] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK =>
        implicit val reads = (__ \ "officers").read[OfficerList]
        val officers = jsonParse(response)("getOfficersHttpReads", Some(regId), Some(transactionId))
        if (officers.items.nonEmpty) officers else {
          errorLog(s"[getOfficersHttpReads] Received an empty Officer list for regId: $regId and txId: $transactionId")
          throw new DownstreamExceptions.OfficerListNotFoundException
        }
      case NOT_FOUND =>
        errorLog(s"[getOfficerList] Received a NotFound status code when expecting an Officer list for regId: $regId and txId: $transactionId")
        throw new DownstreamExceptions.OfficerListNotFoundException
      case status =>
        unexpectedStatusHandling()("getOfficerList", url, status, Some(regId), Some(transactionId))
    }
}

object IncorporationInformationHttpParsers extends IncorporationInformationHttpParsers with BaseConnector
