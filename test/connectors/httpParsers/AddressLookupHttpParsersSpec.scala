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

import ch.qos.logback.classic.Level
import connectors.ALFLocationHeaderNotSetException
import helpers.PayeComponentSpec
import models.Address
import play.api.http.HeaderNames
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.LogCapturingHelper

class AddressLookupHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  val addressId = "12345"

  val testAddress = Address(
    line1 = "Test House",
    line2 = "The Tests",
    line3 = None,
    line4 = None,
    postCode = Some("BB00 0BB"),
    country = None,
    auditRef = None
  )

  val testAddressJson = Json.obj(
    "id" -> "GB200000706253",
    "uprn" -> 706253,
    "parentUprn" -> 706251,
    "usrn" -> 706253,
    "organisation" -> "Some Company",
    "address" -> Json.obj(
      "lines" -> Json.arr(
        "Test House",
        "The Tests"
      ),
      "town" -> "Test Town",
      "postcode" -> "BB00 0BB",
      "subdivision" -> Json.obj(
        "code" -> "GB-ENG",
        "name" -> "England"
      ),
      "country" -> Json.obj(
        "code" -> "GB",
        "name" -> "United Kingdom"
      )
    ),
    "localCustodian" -> Json.obj(
      "code" -> 1760,
      "name" -> "Test Valley"
    ),
    "location" -> Json.arr(
      50.9986451,
      -1.4690977
    ),
    "language" -> "en",
    "administrativeArea" -> "TEST COUNTY"
  )

  "AddressLookupHttpParsers" when {

    "calling .httpAddressReads" when {

      "response is 2xx and JSON is valid" must {

        "return an Address" in {

          AddressLookupHttpParsers.addressHttpReads.read("", "", HttpResponse(OK, json = testAddressJson, Map())) mustBe testAddress
        }
      }

      "response is 2xx and JSON is malformed" must {

        "return a JsResultException and log an error" in {

          withCaptureOfLoggingFrom(AddressLookupHttpParsers.logger) { logs =>
            intercept[JsResultException](AddressLookupHttpParsers.addressHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
            logs.containsMsg(Level.ERROR, "[AddressLookupHttpParsers][addressHttpReads] Address returned from ALF could not be parsed to Address model")
          }
        }
      }

      "response is NOT_FOUND" must {

        "return a NotFoundException and log a warning" in {

          withCaptureOfLoggingFrom(AddressLookupHttpParsers.logger) { logs =>
            intercept[NotFoundException](AddressLookupHttpParsers.addressHttpReads.read("", "", HttpResponse(NOT_FOUND, "")))
            logs.containsMsg(Level.WARN,"[AddressLookupHttpParsers][addressHttpReads] Address could not be found for the supplied journey ID")
          }
        }
      }

      "response is any other status, e.g ISE" must {

        "return an UpstreamErrorResponse and log an error" in {

          withCaptureOfLoggingFrom(AddressLookupHttpParsers.logger) { logs =>
            intercept[UpstreamErrorResponse](AddressLookupHttpParsers.addressHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[AddressLookupHttpParsers][addressHttpReads] Unexpected Error Occurred when calling AddressLookup service. Status '$INTERNAL_SERVER_ERROR'")
          }
        }
      }
    }

    "calling .onRampHttpReads" when {

      "response is 2xx and a Location header is present" must {

        "return the Location header value" in {

          val location = "/foo/bar/wizz"

          AddressLookupHttpParsers.onRampHttpReads.read("", "",
            HttpResponse(ACCEPTED, "", Map(HeaderNames.LOCATION -> Seq(location)))
          ) mustBe location
        }
      }

      "response is 2xx and NO Location header value is present" must {

        "return a ALFLocationHeaderNotSetException and log an ERROR" in {

          withCaptureOfLoggingFrom(AddressLookupHttpParsers.logger) { logs =>
            intercept[ALFLocationHeaderNotSetException](AddressLookupHttpParsers.onRampHttpReads.read("", "", HttpResponse(ACCEPTED, "")))
            logs.containsMsg(Level.ERROR, "[AddressLookupHttpParsers][onRampHttpReads] Location header not set in AddressLookup response")
          }
        }
      }

      "response is any other status, e.g ISE" must {

        "return an UpstreamErrorResponse and log an error" in {

          withCaptureOfLoggingFrom(AddressLookupHttpParsers.logger) { logs =>
            intercept[UpstreamErrorResponse](AddressLookupHttpParsers.onRampHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[AddressLookupHttpParsers][onRampHttpReads] Unexpected Error Occurred when calling AddressLookup service. Status '$INTERNAL_SERVER_ERROR'")
          }
        }
      }
    }
  }
}
