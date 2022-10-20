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
import models.{Address, DigitalContactDetails}
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import play.api.http.HeaderNames
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.LogCapturingHelper

class BusinessRegistrationHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

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
              logs.containsMsg(Level.ERROR, "[businessProfileHttpReads] JSON returned from business-registration could not be parsed to BusinessProfile model")
            }
          }
        }

        "response is NOT_FOUND" must {

          "return a NotFoundException and log a warn message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[NotFoundException](BusinessRegistrationHttpParsers.businessProfileHttpReads.read("", "", HttpResponse(NOT_FOUND, "")))
              logs.containsMsg(Level.WARN, "[businessProfileHttpReads] Business Profile could not be found")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "return an Upstream Error response and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[UpstreamErrorResponse](BusinessRegistrationHttpParsers.businessProfileHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][businessProfileHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
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

          "return an Upstream Error response and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[UpstreamErrorResponse](BusinessRegistrationHttpParsers.retrieveCompletionCapacityHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveCompletionCapacityHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
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

            BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads.read("", "", HttpResponse(OK, json = tradingNameJson, Map())) mustBe Some(tradingName)
          }
        }

        "response is OK and JSON is malformed" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "return None and log an info message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveTradingNameHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe None
              logs.containsMsg(Level.INFO, s"[BusinessRegistrationHttpParsers][retrieveTradingNameHttpReads] No Trading name retrieved from business-registration pre-pop")
            }
          }
        }
      }

      "calling .upsertTradingNameHttpReads(tradingName: String)" when {

        "response is OK" must {

          "return the Trading Name" in {

            BusinessRegistrationHttpParsers.upsertTradingNameHttpReads(tradingName).read("", "", HttpResponse(OK, "")) mustBe tradingName
          }
        }

        "response is any other status, e.g ISE" must {

          "return the Trading Name but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertTradingNameHttpReads(tradingName).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe tradingName
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertTradingNameHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
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

            BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads.read("", "", HttpResponse(OK, json = payeContactDetailsJson, Map())) mustBe Some(payeContactDetails)
          }
        }

        "response is OK and JSON is malformed" must {

          "return a JsResultException and log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, "[BusinessRegistrationHttpParsers][retrieveContactDetailsHttpReads] JSON returned from business-registration could not be parsed to PAYEContactDetails model")
            }
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {

            BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads.read("", "", HttpResponse(NOT_FOUND, json = Json.obj(), Map())) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "return a None but log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveContactDetailsHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe None
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveContactDetailsHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }

      "calling .upsertContactDetailsHttpReads(contactDetails: PAYEContactDetails)" when {

        "response is OK" must {

          "return the PAYE Contact Details" in {

            BusinessRegistrationHttpParsers.upsertContactDetailsHttpReads(payeContactDetails).read("", "", HttpResponse(OK, "")) mustBe payeContactDetails
          }
        }

        "response is any other status, e.g ISE" must {

          "return PAYE Contact Details but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertContactDetailsHttpReads(payeContactDetails).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe payeContactDetails
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertContactDetailsHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
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

            BusinessRegistrationHttpParsers.retrieveAddressesHttpReads.read("", "", HttpResponse(OK, json = addressesJson, Map())) mustBe Seq(address, address)
          }
        }

        "response is OK and JSON is malformed" must {

          "return a JsResultException and log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](BusinessRegistrationHttpParsers.retrieveAddressesHttpReads.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, "[BusinessRegistrationHttpParsers][retrieveAddressesHttpReads] JSON returned from business-registration could not be parsed to Seq[Address] model")
            }
          }
        }

        "response is NOT_FOUND" must {

          "return an empty Sequence" in {

            BusinessRegistrationHttpParsers.retrieveAddressesHttpReads.read("", "", HttpResponse(NOT_FOUND, "")) mustBe Seq()
          }
        }

        "response is any other status, e.g ISE" must {

          "return an empty sequence but log an error" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.retrieveAddressesHttpReads.read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe Seq()
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][retrieveAddressesHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }

      "calling .upsertAddressHttpReads(address: Address)" when {

        "response is OK" must {

          "return the PAYE Contact Details" in {

            BusinessRegistrationHttpParsers.upsertAddressHttpReads(address).read("", "", HttpResponse(OK, "")) mustBe address
          }
        }

        "response is any other status, e.g ISE" must {

          "return PAYE Contact Details but log an error message" in {

            withCaptureOfLoggingFrom(BusinessRegistrationHttpParsers.logger) { logs =>
              BusinessRegistrationHttpParsers.upsertAddressHttpReads(address).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe address
              logs.containsMsg(Level.ERROR, s"[BusinessRegistrationHttpParsers][upsertAddressHttpReads] Unexpected Error Occurred when calling business-registration service. Status '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }
    }
  }
}
