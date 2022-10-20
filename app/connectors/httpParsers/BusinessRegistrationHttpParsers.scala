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

import models.Address
import models.external.BusinessProfile
import models.view.{CompanyDetails, PAYEContactDetails}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Reads}
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpReads, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.Logging

import scala.util.{Failure, Success, Try}

trait BusinessRegistrationHttpParsers extends Logging with HttpErrorFunctions {

  val businessProfileHttpReads: HttpReads[BusinessProfile] = (_: String, _: String, response: HttpResponse) => response.status match {
      case OK =>
        Try(response.json.as[BusinessProfile]) match {
          case Success(business) => business
          case Failure(e) =>
            logger.error("[businessProfileHttpReads] JSON returned from business-registration could not be parsed to BusinessProfile model")
            throw e
        }
      case NOT_FOUND =>
        logger.warn("[businessProfileHttpReads] Business Profile could not be found")
        throw new NotFoundException("Business Profile could not be found")
      case status =>
        logger.error(s"[businessProfileHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
        throw UpstreamErrorResponse("Unexpected Error Occurred when calling business-registration service", status)
    }

  val retrieveCompletionCapacityHttpReads: HttpReads[Option[String]] = (_: String, _: String, response: HttpResponse) => response.status match {
      case OK =>
        (response.json \ "completionCapacity").asOpt[String]
      case NOT_FOUND =>
        None
      case status =>
        logger.error(s"[retrieveCompletionCapacityHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
        throw UpstreamErrorResponse("Unexpected Error Occurred when calling business-registration service", status)
    }

  val retrieveTradingNameHttpReads: HttpReads[Option[String]] = (_: String, _: String, response: HttpResponse) => response.status match {
      case OK =>
        response.json.as[Option[String]](CompanyDetails.tradingNameApiPrePopReads)
      case _ =>
        logger.info(s"[retrieveTradingNameHttpReads] No Trading name retrieved from business-registration pre-pop")
        None
    }

  def upsertTradingNameHttpReads(tradingName: String): HttpReads[String] = (_: String, _: String, response: HttpResponse) => response.status match {
    case OK => tradingName
    case status =>
      logger.error(s"[upsertTradingNameHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
      tradingName
  }

  val retrieveContactDetailsHttpReads: HttpReads[Option[PAYEContactDetails]] = (_: String, _: String, response: HttpResponse) => response.status match {
    case OK =>
      Try(response.json.as[PAYEContactDetails](PAYEContactDetails.prepopReads)) match {
        case Success(contactDetails) => Some(contactDetails)
        case Failure(e) =>
          logger.error("[retrieveContactDetailsHttpReads] JSON returned from business-registration could not be parsed to PAYEContactDetails model")
          throw e
      }
    case NOT_FOUND =>
      None
    case status =>
      logger.error(s"[retrieveContactDetailsHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
      None
  }

  def upsertContactDetailsHttpReads(contactDetails: PAYEContactDetails): HttpReads[PAYEContactDetails] = (_: String, _: String, response: HttpResponse) => response.status match {
    case OK => contactDetails
    case status =>
      logger.error(s"[upsertContactDetailsHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
      contactDetails
  }

  val retrieveAddressesHttpReads: HttpReads[Seq[Address]] = (_: String, _: String, response: HttpResponse) => response.status match {
    case OK =>
      Try((response.json \ "addresses").as[Seq[Address]](Reads.seq(Address.prePopReads))) match {
        case Success(addresses) => addresses
        case Failure(e) =>
          logger.error("[retrieveAddressesHttpReads] JSON returned from business-registration could not be parsed to Seq[Address] model")
          throw e
      }
    case NOT_FOUND =>
      Seq()
    case status =>
      logger.error(s"[retrieveAddressesHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
      Seq()
  }

  def upsertAddressHttpReads(address: Address): HttpReads[Address] = (_: String, _: String, response: HttpResponse) => response.status match {
    case OK => address
    case status =>
      logger.error(s"[upsertAddressHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$status'")
      address
  }
}

object BusinessRegistrationHttpParsers extends BusinessRegistrationHttpParsers
