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

package services

import common.exceptions.DownstreamExceptions.S4LFetchException
import connectors.BusinessRegistrationConnector
import enums.CacheKeys
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PrepopulationServiceImpl @Inject()(val busRegConnector: BusinessRegistrationConnector,
                                         val s4LService: S4LService)(implicit val ec: ExecutionContext) extends PrepopulationService

trait PrepopulationService {
  val busRegConnector: BusinessRegistrationConnector
  val s4LService: S4LService
  implicit val ec: ExecutionContext

  def getBusinessContactDetails(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[DigitalContactDetails]] = {
    busRegConnector.retrieveContactDetails(regId) map {
      case Some(contactDetails) => Some(DigitalContactDetails(contactDetails.digitalContactDetails.email,
        contactDetails.digitalContactDetails.mobileNumber,
        contactDetails.digitalContactDetails.phoneNumber))
      case None => None
    }
  }

  def getPAYEContactDetails(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[PAYEContactDetails]] = {
    busRegConnector.retrieveContactDetails(regId)
  }

  def saveContactDetails(regId: String, contactDetails: PAYEContactDetails)
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[PAYEContactDetails] = {
    busRegConnector.upsertContactDetails(regId, contactDetails) map (_ => contactDetails)
  }

  def getTradingName(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] = {
    busRegConnector.retrieveTradingName(regId)
  }

  def saveTradingName(regId: String, tradingName: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[String] = {
    busRegConnector.upsertTradingName(regId, tradingName)
  }

  def getPrePopAddresses(regId: String, roAddress: Address, ppobAddress: Option[Address], otherAddress: Option[Address])
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[Map[Int, Address]] = {
    busRegConnector.retrieveAddresses(regId) flatMap {
      addresses =>
        val filteredAddresses = filterAddresses(addresses, Seq(Some(roAddress), ppobAddress, otherAddress).flatten)
        s4LService.saveIntMap[Address](CacheKeys.PrePopAddresses.toString, filteredAddresses, regId) map {
          _ => filteredAddresses
        }
    }
  }

  private[services] def filterAddresses(addresses: Seq[Address], addressesToExclude: Seq[Address]): Map[Int, Address] = {
    addresses
      .distinct
      .filterNot(addr => addressesToExclude.contains(addr))
      .zipWithIndex.map {
      _.swap
    }.toMap
  }

  def saveAddress(regId: String, address: Address)(implicit hc: HeaderCarrier, request: Request[_]): Future[Address] = {
    busRegConnector.upsertAddress(regId, address) map {
      _ => address
    }
  }

  def getAddress(regId: String, addressId: Int)(implicit hc: HeaderCarrier): Future[Address] = {
    s4LService.fetchAndGetIntMap[Address](CacheKeys.PrePopAddresses.toString, regId) map {
      oAddresses =>
        oAddresses.map {
          _.getOrElse(addressId, {
            throw new S4LFetchException(s"[PrepopulationService] - [getAddress] No address stored with address ID $addressId for reg ID $regId")
          })
        }.getOrElse(throw new S4LFetchException(s"[PrepopulationService] - [getAddress] No address map returned from S4L for reg ID $regId"))
    }
  }
}
