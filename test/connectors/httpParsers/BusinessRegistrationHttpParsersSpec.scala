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

import ch.qos.logback.classic.Level
import common.exceptions.DownstreamExceptions
import connectors.ALFLocationHeaderNotSetException
import helpers.PayeComponentSpec
import models.{Address, DigitalContactDetails}
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import play.api.http.HeaderNames
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.LogCapturingHelper

class BusinessRegistrationHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  implicit val request: Request[_] = fakeRequest()

  val regId = "reg1234"

  "BusinessRegistrationHttpParsers" when {

    "For Business Profile" when {

      "calling .businessProfileHttpReads" when {

        val businessProfile = BusinessProfile("12345", "EN")
        val businessProfileJson = Json.obj(
          "registrationID" -> "12345",
          "language" -> "EN"
        )

        "response is OK and JSON is valid" must {

          "return the Business Profile" in {

            BusinessRegistrationHttpParsers.businessProfileHttpReads.read("", "", HttpResponse(OK, json = businessProfileJson, Map())) mustBe businessProfile
          }
        }

        "response is OK and JSON is malformed" must {

          "return a JsResultException and log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](BusinessRegistrationHttpParsers.businessProfileHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, "[businessProfileHttpReads] JSON returned could not be parsed to models.external.BusinessProfile model")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "return an Upstream Error response and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.BusinessRegistrationException](BusinessRegistrationHttpParsers.businessProfileHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][businessProfileHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }

      "calling .retrieveCompletionCapacityHttpReads" when {

        val completionCapacityJson = Json.obj(
          "completionCapacity" -> "foo"
        )

        "response is OK and JSON is valid" must {

          "return the Completion Capacity" in {

            BusinessRegistrationHttpParsers.retrieveCompletionCapacityHttpReads.read("", "", HttpResponse(OK, json = completionCapacityJson, Map())) mustBe Some("foo")
          }
        }

        "response is OK and JSON is malformed" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveCompletionCapacityHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveCompletionCapacityHttpReads.read("", "", HttpResponse(NOT_FOUND, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "return an BusinessRegistrationException response and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.BusinessRegistrationException](BusinessRegistrationHttpParsers.retrieveCompletionCapacityHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveCompletionCapacityHttpReads] Calling url: '' returned unexpected status: '${INTERNAL_SERVER_ERROR}'")
            }
          }
        }
      }
    }

    "for Trading Name" when {

      val tradingName = "trade"
      val tradingNameJson = Json.obj(
        "tradingName" -> tradingName
      )

      "calling .retrieveTradingNameHttpReads" when {

        "response is OK and JSON is valid" must {

          "return the Trading Name" in {

            BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads(regId).read("", "", HttpResponse(OK, json = tradingNameJson, Map())) mustBe Some(tradingName)
          }
        }

        "response is OK and JSON is malformed" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads(regId).read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "return None and log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads(regId).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe None
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveTradingNameHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertTradingNameHttpReads(regId: String, tradingName: String)" when {

        "response is OK" must {

          "return the Trading Name" in {

            BusinessRegistrationHttpParsers.upsertTradingNameHttpReads(regId, tradingName).read("", "", HttpResponse(OK, "")) mustBe tradingName
          }
        }

        "response is any other status, e.g ISE" must {

          "return the Trading Name but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertTradingNameHttpReads(regId, tradingName).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe tradingName
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertTradingNameHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "for Contact Details" when {

      val payeContactDetails = PAYEContactDetails(
        "First Middle Last",
        DigitalContactDetails(
          Some("test@example.com"),
          Some("01234567890"),
          Some("01234567890")
        )
      )
      val payeContactDetailsJson = Json.obj(
        "firstName" -> "First",
        "middleName" -> "Middle",
        "surname" -> "Last",
        "email" -> "test@example.com",
        "telephoneNumber" -> "01234567890",
        "mobileNumber" -> "01234567890"
      )

      "calling .retrieveContactDetailsHttpReads" when {

        "response is OK and JSON is valid" must {

          "return the Contact Details" in {

            BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads(regId).read("", "", HttpResponse(OK, json = payeContactDetailsJson, Map())) mustBe Some(payeContactDetails)
          }
        }

        "response is CREATED and JSON is valid" must {

          "return the Contact Details" in {

            BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads(regId).read("", "", HttpResponse(CREATED, json = payeContactDetailsJson, Map())) mustBe Some(payeContactDetails)
          }
        }

        "response is OK and JSON is malformed" must {

          "return None but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads(regId).read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveContactDetailsHttpReads] JSON returned could not be parsed to models.view.PAYEContactDetails model for regId: '$regId'")
            }
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads(regId).read("", "", HttpResponse(NOT_FOUND, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "return a None but log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads(regId).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe None
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveContactDetailsHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertContactDetailsHttpReads(regId: String, contactDetails: PAYEContactDetails)" when {

        "response is OK" must {

          "return the PAYE Contact Details" in {

            BusinessRegistrationHttpParsers.upsertContactDetailsHttpReads(regId, payeContactDetails).read("", "", HttpResponse(OK, "")) mustBe payeContactDetails
          }
        }

        "response is any other status, e.g ISE" must {

          "return PAYE Contact Details but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertContactDetailsHttpReads(regId, payeContactDetails).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe payeContactDetails
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertContactDetailsHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "for Addresses" when {

      val address = Address(
        line1 = "Test House",
        line2 = "The Tests",
        line3 = Some("Line 3"),
        line4 = Some("Line 4"),
        postCode = Some("BB00 0BB"),
        country = None,
        auditRef = Some("ref")
      )
      val addressJson = Json.obj(
        "addressLine1" -> "Test House",
        "addressLine2" -> "The Tests",
        "addressLine3" -> "Line 3",
        "addressLine4" -> "Line 4",
        "postcode" -> "BB00 0BB",
        "auditRef" -> "ref"
      )
      val addressesJson = Json.obj("addresses" -> Json.arr(addressJson, addressJson))

      "calling .retrieveAddressesHttpReads" when {

        "response is OK and JSON is valid" must {

          "return a Sequence of Addresses" in {

            BusinessRegistrationHttpParsers.retrieveAddressesHttpReads(regId).read("", "", HttpResponse(OK, json = addressesJson, Map())) mustBe Seq(address, address)
          }
        }

        "response is OK and JSON is malformed" must {

          "return a JsResultException and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](BusinessRegistrationHttpParsers.retrieveAddressesHttpReads(regId).read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveAddressesHttpReads] JSON returned could not be parsed to scala.collection.Seq model for regId: '$regId'")
            }
          }
        }

        "response is NOT_FOUND" must {

          "return an empty Sequence" in {

            BusinessRegistrationHttpParsers.retrieveAddressesHttpReads(regId).read("", "", HttpResponse(NOT_FOUND, "")) mustBe Seq()
          }
        }

        "response is any other status, e.g ISE" must {

          "return an empty sequence but log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveAddressesHttpReads(regId).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe Seq()
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveAddressesHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertAddressHttpReads(address: Address)" when {

        "response is OK" must {

          "return the PAYE Contact Details" in {

            BusinessRegistrationHttpParsers.upsertAddressHttpReads(regId, address).read("", "", HttpResponse(OK, "")) mustBe address
          }
        }

        "response is any other status, e.g ISE" must {

          "return PAYE Contact Details but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertAddressHttpReads(regId, address).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe address
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertAddressHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }
  }
}
