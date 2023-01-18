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
import connectors.{Cancelled, Success, TimedOut}
import enums.{DownstreamOutcome, PAYEStatus}
import helpers.PayeComponentSpec
import models.api._
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.libs.json.{JsResultException, JsString, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpResponse
import utils.LogCapturingHelper

import java.time.LocalDate

class PAYERegistrationHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  implicit val request: Request[_] = fakeRequest()

  val regId = "reg1234"
  val txId = "tx1234"

  val address = Address(
    line1 = "Line 1",
    line2 = "Line 2",
    line3 = None,
    line4 = None,
    postCode = None
  )

  val digitalContactDetails = DigitalContactDetails(
    email = Some("test@example.com"),
    mobileNumber = None,
    phoneNumber = None
  )

  val companyDetails = CompanyDetails(
    companyName = "Test Ltd",
    tradingName = None,
    roAddress = address,
    ppobAddress = address,
    businessContactDetails = digitalContactDetails
  )

  val companyDetailsJson = Json.toJson(companyDetails)

  val employment = Employment(
    employees = Employing.alreadyEmploying,
    firstPaymentDate = LocalDate.of(2020, 1, 1),
    construction = false,
    subcontractors = false,
    companyPension = None
  )

  val employmentJson = Json.toJson(employment)

  val payeContact = PAYEContact(
    contactDetails = PAYEContactDetails(
      name = "Geoff",
      digitalContactDetails = digitalContactDetails
    ),
    correspondenceAddress = address
  )

  val payeContactJson = Json.toJson(payeContact)

  val sicCodes = List(SICCode(Some("ABC"), Some("Farmers")))

  val sicCodesJson = Json.toJson(sicCodes)

  val directors = List(Director(Name(Some("Geoff"), None, None, None), None))

  val directorsJson = Json.toJson(directors)

  val payeDetails = PAYERegistration(
    registrationID = regId,
    transactionID = txId,
    formCreationTimestamp = "20221015T10:15:00.123Z",
    status = PAYEStatus.submitted,
    completionCapacity = "director",
    companyDetails = companyDetails,
    employmentInfo = employment,
    sicCodes = sicCodes,
    directors = directors,
    payeContact = payeContact
  )

  val payeDetailsJson = Json.toJson(payeDetails)

  "PAYERegistrationHttpParsers" when {

    "For the PAYE Regitstration" when {

      "calling .createNewRegistrationHttpReads" when {

        val rds = PAYERegistrationHttpParsers.createNewRegistrationHttpReads(regId, txId)

        "response is OK" must {

          "return DownstreamOutcome.Success" in {

            rds.read("", "", HttpResponse(OK, "")) mustBe DownstreamOutcome.Success
          }
        }

        "response is any other status, e.g ISE" must {

          "return a DownstreamOutcome.Failure response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "/reg/new", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe DownstreamOutcome.Failure
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][createNewRegistrationHttpReads] Calling url: '/reg/new' returned unexpected status: '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }

      "calling .getRegistrationHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getRegistrationHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return PAYEDetails" in {

            rds.read("", "", HttpResponse(OK, json = payeDetailsJson, Map())) mustBe payeDetails
          }
        }

        "response is OK but JSON is malformed" must {

          "throw a JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getRegistrationHttpReads] JSON returned could not be parsed to models.api.PAYERegistration model for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/payeDetails", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getRegistrationHttpReads] Calling url: '/payeDetails' returned unexpected status: '$INTERNAL_SERVER_ERROR'")
            }
          }
        }
      }

      "calling .getRegistrationIdHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getRegistrationIdHttpReads(txId)

        "response is OK and JSON is valid" must {

          "return RegId" in {

            rds.read("", "", HttpResponse(OK, json = JsString(regId), Map())) mustBe regId
          }
        }

        "response is OK but JSON is malformed" must {

          "throw a JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getRegistrationIdHttpReads] JSON returned could not be parsed to java.lang.String model for txId: '$txId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/regId", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getRegistrationIdHttpReads] Calling url: '/regId' returned unexpected status: '$INTERNAL_SERVER_ERROR' for txId: '$txId'")
            }
          }
        }
      }

      "calling .submitRegistrationHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.submitRegistrationHttpReads(regId)

        "response is OK" must {

          "return Success" in {

            rds.read("", "", HttpResponse(OK, "")) mustBe Success
          }
        }

        "response is NO_CONTENT" must {

          "return Cancelled" in {

            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe Cancelled
          }
        }

        "response is GATEWAY_TIMEOUT" must {

          "return TimeOut and log error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(GATEWAY_TIMEOUT, "")) mustBe TimedOut
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][submitRegistrationHttpReads] Timed out when submitting PAYE Registration to DES for regId: '$regId'")
            }
          }
        }

        "response is REQUEST_TIMEOUT" must {

          "return TimeOut and log error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(REQUEST_TIMEOUT, "")) mustBe TimedOut
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][submitRegistrationHttpReads] Timed out when submitting PAYE Registration to DES for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/submit", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][submitRegistrationHttpReads] Calling url: '/submit' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .getStatusHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getStatusHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return the Status" in {

            rds.read("", "", HttpResponse(OK, json = payeDetailsJson, Map())) mustBe Some(PAYEStatus.submitted)
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getStatusHttpReads] JSON returned could not be parsed to scala.Enumeration$$Value model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/status", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getStatusHttpReads] Calling url: '/status' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

    }

    "For Company Details" when {

      "calling .getCompanyDetailsHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getCompanyDetailsHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return CompanyDetails" in {

            rds.read("", "", HttpResponse(OK, json = companyDetailsJson, Map())) mustBe Some(companyDetails)
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getCompanyDetailsHttpReads] JSON returned could not be parsed to models.api.CompanyDetails model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/companyDeetz", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getCompanyDetailsHttpReads] Calling url: '/companyDeetz' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertCompanyDetailsHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.upsertCompanyDetailsHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return CompanyDetails" in {

            rds.read("", "", HttpResponse(OK, json = companyDetailsJson, Map())) mustBe companyDetails
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertCompanyDetailsHttpReads] JSON returned could not be parsed to models.api.CompanyDetails model for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/companyDeetz", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertCompanyDetailsHttpReads] Calling url: '/companyDeetz' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For Employment Details" when {

      "calling .getEmploymentHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getEmploymentHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return EmploymentDetails" in {

            rds.read("", "", HttpResponse(OK, json = employmentJson, Map())) mustBe Some(employment)
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getEmploymentHttpReads] JSON returned could not be parsed to models.api.Employment model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/employment", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getEmploymentHttpReads] Calling url: '/employment' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertEmploymentHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.upsertEmploymentHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return EmploymentDetails" in {

            rds.read("", "", HttpResponse(OK, json = employmentJson, Map())) mustBe employment
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertEmploymentHttpReads] JSON returned could not be parsed to models.api.Employment model for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/employment", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertEmploymentHttpReads] Calling url: '/employment' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For PAYE Contact Details" when {

      "calling .getPAYEContactHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getPAYEContactHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return PAYEContactDetails" in {

            rds.read("", "", HttpResponse(OK, json = payeContactJson, Map())) mustBe Some(payeContact)
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getPAYEContactHttpReads] JSON returned could not be parsed to models.api.PAYEContact model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/payeContactDeetz", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getPAYEContactHttpReads] Calling url: '/payeContactDeetz' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertPAYEContactHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.upsertPAYEContactHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return PAYEContactDetails" in {

            rds.read("", "", HttpResponse(OK, json = payeContactJson, Map())) mustBe payeContact
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertPAYEContactHttpReads] JSON returned could not be parsed to models.api.PAYEContact model for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/payeContactDeetz", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertPAYEContactHttpReads] Calling url: '/payeContactDeetz' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For Completion Capacity" when {

      "calling .getCompletionCapacityHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getCompletionCapacityHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return EmploymentDetails" in {

            rds.read("", "", HttpResponse(OK, json = JsString("foo"), Map())) mustBe Some("foo")
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getCompletionCapacityHttpReads] JSON returned could not be parsed to java.lang.String model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/completionCapacity", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getCompletionCapacityHttpReads] Calling url: '/completionCapacity' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }

      "calling .upsertCompletionCapacityHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.upsertCompletionCapacityHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return CompletionCapacity" in {

            rds.read("", "", HttpResponse(OK, json = JsString("foo"), Map())) mustBe "foo"
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertCompletionCapacityHttpReads] JSON returned could not be parsed to java.lang.String model for regId: '$regId'")
            }
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/completionCapacity", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][upsertCompletionCapacityHttpReads] Calling url: '/completionCapacity' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For AcknowledgementReference" when {

      "calling .getAcknowledgementReferenceHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.getAcknowledgementReferenceHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return the AcknowledgementReference" in {

            rds.read("", "", HttpResponse(OK, json = JsString("ref1234"), Map())) mustBe Some("ref1234")
          }
        }

        "response is OK but JSON is malformed" must {

          "return None but log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getAcknowledgementReferenceHttpReads] JSON returned could not be parsed to java.lang.String model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return None" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
          }
        }

        "response is NOT_FOUND" must {

          "return None" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/ackRef", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][getAcknowledgementReferenceHttpReads] Calling url: '/ackRef' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For SIC Codes" when {

      "calling .sicCodesHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.sicCodesHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return the AcknowledgementReference" in {

            rds.read("", "", HttpResponse(OK, json = sicCodesJson, Map())) mustBe sicCodes
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][sicCodesHttpReads] JSON returned could not be parsed to scala.collection.Seq model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return Seq()" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe Seq()
          }
        }

        "response is NOT_FOUND" must {

          "return Seq()" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe Seq()
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/sicCodes", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][sicCodesHttpReads] Calling url: '/sicCodes' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }

    "For Directors" when {

      "calling .directorsHttpReads(regId)" when {

        val rds = PAYERegistrationHttpParsers.directorsHttpReads(regId)

        "response is OK and JSON is valid" must {

          "return the AcknowledgementReference" in {

            rds.read("", "", HttpResponse(OK, json = directorsJson, Map())) mustBe directors
          }
        }

        "response is OK but JSON is malformed" must {

          "throw JsResultException and log an error" in {
            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[JsResultException](rds.read("", "", HttpResponse(OK, json = Json.obj(), Map())))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][directorsHttpReads] JSON returned could not be parsed to scala.collection.Seq model for regId: '$regId'")
            }
          }
        }

        "response is NO_CONTENT" must {

          "return Seq()" in {
            rds.read("", "", HttpResponse(NO_CONTENT, "")) mustBe Seq()
          }
        }

        "response is NOT_FOUND" must {

          "return Seq()" in {
            rds.read("", "", HttpResponse(NOT_FOUND, "")) mustBe Seq()
          }
        }

        "response is any other status, e.g ISE" must {

          "throw a DownstreamExceptions.PAYEMicroserviceException response and log an error" in {

            withCaptureOfLoggingFrom(PAYERegistrationHttpParsers.logger) { logs =>
              intercept[DownstreamExceptions.PAYEMicroserviceException](rds.read("", "/directors", HttpResponse(INTERNAL_SERVER_ERROR, "")))
              logs.containsMsg(Level.ERROR, s"[PAYERegistrationHttpParsers][directorsHttpReads] Calling url: '/directors' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
            }
          }
        }
      }
    }
  }
}
