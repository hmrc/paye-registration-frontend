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

import connectors.ALFLocationHeaderNotSetException
import models.Address
import play.api.http.HeaderNames
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpReads, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.Logging

import scala.util.{Failure, Success, Try}

trait AddressLookupHttpParsers extends Logging {

  val addressHttpReads = new HttpReads[Address] {
    override def read(method: String, url: String, response: HttpResponse): Address = response.status match {
      case status if HttpErrorFunctions.is2xx(status) =>
        Try(response.json.as[Address](Address.addressLookupReads)) match {
          case Success(address) => address
          case Failure(e) =>
            logger.error("[addressHttpReads] Address returned from ALF could not be parsed to Address model")
            throw e
        }
      case NOT_FOUND =>
        logger.warn("[addressHttpReads] Address could not be found for the supplied journey ID")
        throw new NotFoundException("Address could not be found for the supplied journey ID")
      case status =>
        logger.error(s"[addressHttpReads] Unexpected Error Occurred when calling AddressLookup service. Status '$status'")
        throw UpstreamErrorResponse("Unexpected Error Occurred when calling AddressLookup service", status)
    }
  }

  val onRampHttpReads = new HttpReads[String] {

    override def read(method: String, url: String, response: HttpResponse): String = response.status match {
      case status if HttpErrorFunctions.is2xx(status) =>
        response.header(HeaderNames.LOCATION).getOrElse {
          logger.error("[onRampHttpReads] Location header not set in AddressLookup response")
          throw new ALFLocationHeaderNotSetException
        }
      case status =>
        logger.error(s"[onRampHttpReads] Unexpected Error Occurred when calling AddressLookup service. Status '$status'")
        throw UpstreamErrorResponse("Unexpected Error Occurred when calling AddressLookup service", status)
    }
  }

}

object AddressLookupHttpParsers extends AddressLookupHttpParsers
