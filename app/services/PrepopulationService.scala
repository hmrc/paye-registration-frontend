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

import common.exceptions.DownstreamExceptions.S4LFetchException
import connectors.{BusinessRegistrationConnect, BusinessRegistrationConnector}
import enums.CacheKeys
import models.{Address, DigitalContactDetails}
import models.view.PAYEContactDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class
PrepopulationService @Inject()(injBusinessRegistrationConnector: BusinessRegistrationConnector,
                                     injS4LService: S4LService) extends PrepopulationSrv {
  override val busRegConnector = injBusinessRegistrationConnector
  override val s4LService = injS4LService
}

trait PrepopulationSrv {
  val busRegConnector: BusinessRegistrationConnect
  val s4LService: S4LSrv

  def getBusinessContactDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[DigitalContactDetails]] = {
    busRegConnector.retrieveContactDetails(regId) map {
      case Some(contactDetails) => Some(DigitalContactDetails(contactDetails.digitalContactDetails.email,
                                                              contactDetails.digitalContactDetails.mobileNumber,
                                                              contactDetails.digitalContactDetails.phoneNumber))
      case None => None
    }
  }

  def getPAYEContactDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[PAYEContactDetails]] = {
    busRegConnector.retrieveContactDetails(regId)
  }

  def saveContactDetails(regId: String, contactDetails: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[PAYEContactDetails] = {
    busRegConnector.upsertContactDetails(regId, contactDetails) map {
      _ => contactDetails
    }
  }

  def getPrePopAddresses(regId: String, roAddress: Address, otherAddress: Option[Address])(implicit hc: HeaderCarrier): Future[Map[Int,Address]] = {
    busRegConnector.retrieveAddresses(regId) flatMap {
      addresses =>
        val filteredAddresses = filterAddresses(addresses, roAddress, otherAddress)
        s4LService.saveIntMap[Address](CacheKeys.PrePopAddresses.toString, filteredAddresses, regId) map {
        _ => filteredAddresses
      }
    }
  }

  private[services] def filterAddresses(addresses: Seq[Address], roAddress: Address, otherAddress: Option[Address]): Map[Int, Address] = {
    addresses
      .distinct
      .filterNot(addr => roAddress == addr || otherAddress.contains(addr))
      .zipWithIndex.map{_.swap}.toMap
  }

  def saveAddress(regId: String, address: Address)(implicit hc: HeaderCarrier): Future[Address] = {
    busRegConnector.upsertAddress(regId, address) map {
      _ => address
    }
  }

  def getAddress(regId: String, addressId: Int)(implicit hc: HeaderCarrier): Future[Address] = {
    s4LService.fetchAndGetIntMap[Address](CacheKeys.PrePopAddresses.toString, regId) map {
      oAddresses => oAddresses.map {
        _.getOrElse(addressId, {throw new S4LFetchException(s"[PrepopulationService] - [getAddress] No address stored with address ID $addressId for reg ID $regId")})
      }.getOrElse(throw new S4LFetchException(s"[PrepopulationService] - [getAddress] No address map returned from S4L for reg ID $regId"))
    }
  }
}

