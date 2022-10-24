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

import common.exceptions
import connectors.{ALFLocationHeaderNotSetException, BaseConnector}
import models.Address
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.{Failure, Success, Try}

trait AddressLookupHttpParsers extends BaseHttpReads { _ : BaseConnector =>

  override def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new exceptions.DownstreamExceptions.AddressLookupException(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  val addressHttpReads: HttpReads[Address] = (_: String, url: String, response: HttpResponse) => response.status match {
    case status if is2xx(status) =>
      Try(response.json.as[Address](Address.addressLookupReads)) match {
        case Success(address) => address
        case Failure(e) =>
          logger.error("[addressHttpReads] Address returned from ALF could not be parsed to Address model")
          throw e
      }
    case status =>
      unexpectedStatusHandling()("addressHttpReads", url, status)
  }

  val onRampHttpReads: HttpReads[String] = (_: String, url: String, response: HttpResponse) => response.status match {
    case status if is2xx(status) =>
      response.header(HeaderNames.LOCATION).getOrElse {
        logger.error("[onRampHttpReads] Location header not set in AddressLookup response")
        throw new ALFLocationHeaderNotSetException
      }
    case status =>
      unexpectedStatusHandling()("onRampHttpReads", url, status)
  }
}

object AddressLookupHttpParsers extends AddressLookupHttpParsers with BaseConnector
