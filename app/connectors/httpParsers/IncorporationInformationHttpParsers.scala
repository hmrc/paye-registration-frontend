/*
 * Copyright 2022 HM Revenue & Customs
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

import common.exceptions.DownstreamExceptions
import connectors.{IncorpInfoBadRequestResponse, IncorpInfoNotFoundResponse, IncorpInfoResponse, IncorpInfoSuccessResponse}
import enums.IncorporationStatus
import models.external.{CoHoCompanyDetailsModel, IncorpUpdateResponse, OfficerList}
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, NOT_FOUND, NO_CONTENT, OK}
import uk.gov.hmrc.http.{BadRequestException, HttpErrorFunctions, HttpReads, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.Logging

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

trait IncorporationInformationHttpParsers extends Logging with HttpErrorFunctions {

  def setupSubscriptionHttpReads(regId: String, transactionId: String, subscriber: String, regime: String): HttpReads[Option[IncorporationStatus.Value]] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => Try(response.json.as[IncorporationStatus.Value](IncorpUpdateResponse.reads(transactionId, subscriber, regime))) match {
        case Success(status) => Some(status)
        case Failure(e) =>
          logger.error(s"[setupSubscriptionHttpReads] JSON returned from incorporation-information could not be parsed regId: $regId and txId: $transactionId")
          throw e
      }
      case ACCEPTED =>
        None
      case status =>
        unexpectedStatusHandling("setupSubscription", regId, transactionId, url, status)
    }

  def cancelSubscriptionHttpReads(regId: String, transactionId: String): HttpReads[Boolean] = (_: String, url: String, response: HttpResponse) => response.status match {
    case OK => true
    case NOT_FOUND =>
      logger.info(s"[cancelSubscriptionHttpReads] no subscription found when trying to delete subscription. it might already have been deleted for regId: $regId and txId: $transactionId")
      true
    case status =>
      unexpectedStatusHandling("cancelSubscriptionHttpReads", regId, transactionId, url, status, defaultResult = Some(false))
  }

  def getCoHoCompanyDetailsHttpReads(regId: String, transactionId: String): HttpReads[IncorpInfoResponse] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK => Try(response.json.as[CoHoCompanyDetailsModel](CoHoCompanyDetailsModel.incorpInfoReads)) match {
        case Success(coHoCompanyDetailsModel) =>
          IncorpInfoSuccessResponse(coHoCompanyDetailsModel)
        case Failure(e) =>
          logger.error(s"[getCoHoCompanyDetailsHttpReads] JSON returned from incorporation-information could not be parsed for regId: $regId and txId: $transactionId")
          throw e
      }
      case BAD_REQUEST =>
        logger.error(s"[getCoHoCompanyDetailsHttpReads] Received a BadRequest status code when expecting company details for regId: $regId and txId: $transactionId")
        IncorpInfoBadRequestResponse
      case NOT_FOUND =>
        logger.error(s"[getCoHoCompanyDetailsHttpReads] Received a NotFound status code when expecting company details for regId: $regId and txId: $transactionId")
        IncorpInfoNotFoundResponse
      case status =>
        unexpectedStatusHandling("getCoHoCompanyDetailsHttpReads", regId, transactionId, url, status)
    }

  def getIncorpInfoDateHttpReads(regId: String, transactionId: String): HttpReads[Option[LocalDate]] = (_: String, url: String, response: HttpResponse) => response.status match {
    case OK =>
      (response.json \ "incorporationDate").asOpt[String] match {
        case Some(potentialDate) => Try(LocalDate.parse(potentialDate)) match {
          case Success(date) => Some(date)
          case Failure(e) =>
            logger.error(s"[getIncorpInfoDateHttpReads] IncorpDate was retrieved from II but was not able to be parsed to LocalDate format. Value received: '$potentialDate' for regId: $regId and txId: $transactionId")
            None
        }
        case None =>
          logger.info(s"[getIncorpInfoDateHttpReads] No IncorpDate was retrieved from II for regId: $regId and txId: $transactionId")
          None
      }
    case NO_CONTENT =>
      None
    case status =>
      unexpectedStatusHandling("getIncorpInfoDateHttpReads", regId, transactionId, url, status)
  }

  def getOfficersHttpReads(regId: String, transactionId: String): HttpReads[OfficerList] =
    (_: String, url: String, response: HttpResponse) => response.status match {
      case OK =>
        Try((response.json \ "officers").as[OfficerList]) match {
          case Success(officers) if officers.items.nonEmpty => officers
          case Success(_) =>
              logger.error(s"[getOfficersHttpReads] Received an empty Officer list for regId: $regId and txId: $transactionId")
              throw new DownstreamExceptions.OfficerListNotFoundException
          case Failure(e) =>
            logger.error(s"[getOfficersHttpReads] JSON returned from incorporation-information could not be parsed for regId: $regId and txId: $transactionId")
            throw e
        }
      case NOT_FOUND =>
        logger.error(s"[getOfficerList] Received a NotFound status code when expecting an Officer list for regId: $regId and txId: $transactionId")
        throw new DownstreamExceptions.OfficerListNotFoundException
      case status =>
        unexpectedStatusHandling("getOfficerList", regId, transactionId, url, status)
    }

  private def unexpectedStatusHandling[T](functionName: String, regId: String, transactionId: String, url: String, status: Int, defaultResult: => Option[T] = None): T = {
    logger.error(s"[$functionName] An unexpected response was received when calling II for regId: $regId and txId: $transactionId. Status: '$status'")
    defaultResult.fold(throw new DownstreamExceptions.IncorporationInformationResponseException(s"Calling II on $url returned status: '$status'"))(identity)
  }
}

object IncorporationInformationHttpParsers extends IncorporationInformationHttpParsers
