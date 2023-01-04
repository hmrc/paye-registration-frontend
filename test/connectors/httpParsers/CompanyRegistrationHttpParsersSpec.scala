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
import helpers.PayeComponentSpec
import models.external.CompanyRegistrationProfile
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.LogCapturingHelper

class CompanyRegistrationHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  val regId = "reg12345"

  "CompanyRegistrationHttpParsers" when {

    "calling .companyRegistrationDetailsHttpReads(regId: String)" when {

      val status = "SUCCESS"
      val txId = "txn1234"
      val paidIncorporation = "payt1234"
      val ackStatus = "ack1234"

      val companyRegistrationProfile = CompanyRegistrationProfile(
        status = status,
        transactionId = txId,
        ackRefStatus = Some(ackStatus),
        paidIncorporation = Some(paidIncorporation)
      )

      val companyRegistrationProfileJson = Json.obj(
        "status" -> status,
        "confirmationReferences" -> Json.obj(
          "payment-reference" -> paidIncorporation,
          "transaction-id" -> txId
        ),
        "acknowledgementReferences" -> Json.obj(
          "status" -> ackStatus
        )
      )

      val malformedJson = Json.obj(
        "stdddatus" -> status,
        "confirmationReferences" -> Json.obj(
          "payment-reference" -> paidIncorporation,
          "transaction-id" -> txId
        ),
        "acknowledgementReferences" -> Json.obj(
          "status" -> ackStatus
        )
      )

      "response is OK and JSON is valid" must {

        "return the Company Registration" in {

          CompanyRegistrationHttpParsers.companyRegistrationDetailsHttpReads(regId).read("", "", HttpResponse(OK, json = companyRegistrationProfileJson, Map())) mustBe companyRegistrationProfile
        }
      }

      "response is OK, JSON is valid BUT transaction-id is missing" must {

        "throw a DownstreamExceptions.ConfirmationRefsNotFoundException and log an error message" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.ConfirmationRefsNotFoundException](
              CompanyRegistrationHttpParsers.companyRegistrationDetailsHttpReads(regId).read("", "", HttpResponse(OK, json = Json.obj(), Map()))
            )
            logs.containsMsg(Level.ERROR, s"[companyRegistrationDetailsHttpReads] Received an error when expecting a Company Registration document for reg id: $regId could not find confirmation references (has user completed Incorp/CT?)")
          }
        }
      }

      "response is OK and JSON is malformed" must {

        "return a JsResultException and log an error message" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            intercept[JsResultException](CompanyRegistrationHttpParsers.companyRegistrationDetailsHttpReads(regId).read("", "", HttpResponse(OK, json = malformedJson, Map())))
            logs.containsMsg(Level.ERROR, s"[companyRegistrationDetailsHttpReads] JSON returned from company-registration could not be parsed to CompanyRegistrationProfile model for reg id: $regId")
          }
        }
      }

      "response is any other status, e.g ISE" must {

        "return an Upstream Error response and log an error" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.CompanyRegistrationException](CompanyRegistrationHttpParsers.companyRegistrationDetailsHttpReads(regId).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[companyRegistrationDetailsHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
          }
        }
      }
    }

    "calling .verifiedEmailHttpReads(regId: String)" when {

      val email = "test@example.com"
      val verifiedEmailJson = Json.obj("address" -> email)

      "response is OK and JSON is valid" must {

        "return the Verified Email" in {

          CompanyRegistrationHttpParsers.verifiedEmailHttpReads(regId).read("", "", HttpResponse(OK, json = verifiedEmailJson, Map())) mustBe Some(email)
        }
      }

      "response is OK and JSON is malformed" must {

        "return a None and log an info message" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            CompanyRegistrationHttpParsers.verifiedEmailHttpReads(regId).read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None
            logs.containsMsg(Level.INFO, s"[verifiedEmailHttpReads] A response for the verified email was returned but did not contain the 'address' object for regId: $regId")
          }
        }
      }

      "response is NOT_FOUND" must {

        "return a None and log an info message" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            CompanyRegistrationHttpParsers.verifiedEmailHttpReads(regId).read("", "", HttpResponse(NOT_FOUND, "")) mustBe None
            logs.containsMsg(Level.INFO, s"[verifiedEmailHttpReads] A call was made to company reg and a NotFound response was returned for regId: $regId")
          }
        }
      }

      "response is any other status, e.g ISE" must {

        "return a None and log an Error message" in {

          withCaptureOfLoggingFrom(CompanyRegistrationHttpParsers.logger) { logs =>
            CompanyRegistrationHttpParsers.verifiedEmailHttpReads(regId).read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe None
            logs.containsMsg(Level.ERROR, s"[verifiedEmailHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId'")
          }
        }
      }
    }
  }
}
