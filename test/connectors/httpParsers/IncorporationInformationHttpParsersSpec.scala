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
import common.exceptions.DownstreamExceptions
import connectors.{IncorpInfoBadRequestResponse, IncorpInfoNotFoundResponse, IncorpInfoSuccessResponse}
import enums.IncorporationStatus
import helpers.PayeComponentSpec
import models.Address
import models.api.Name
import models.external.{CoHoCompanyDetailsModel, Officer, OfficerList}
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.HttpResponse
import utils.LogCapturingHelper

import java.time.LocalDate

class IncorporationInformationHttpParsersSpec extends PayeComponentSpec with LogCapturingHelper {

  val regId: String = "reg1234"
  val transactionId: String = "txn1234"
  val subscriber: String = "sub"
  val regime: String = "regime"

  "IncorporationInformationHttpParsers" when {

    "calling .setupSubscriptionHttpReads" when {

      val incorStatusResponseJson = Json.obj(
        "SCRSIncorpStatus" -> Json.obj(
          "IncorpSubscriptionKey" -> Json.obj(
            "transactionId" -> transactionId,
            "subscriber" -> subscriber,
            "discriminator" -> regime
          ),
          "IncorpStatusEvent" -> Json.obj(
            "status" -> "rejected"
          )
        )
      )

      val incorStatusResponseJsonWrongUser = Json.obj(
        "SCRSIncorpStatus" -> Json.obj(
          "IncorpSubscriptionKey" -> Json.obj(
            "transactionId" -> transactionId,
            "subscriber" -> "foobar",
            "discriminator" -> regime
          ),
          "IncorpStatusEvent" -> Json.obj(
            "status" -> "rejected"
          )
        )
      )

      "response is 2xx and JSON is valid" must {

        "return the Status" in {

          IncorporationInformationHttpParsers.setupSubscriptionHttpReads(regId, transactionId, subscriber, regime)
            .read("", "", HttpResponse(OK, json = incorStatusResponseJson, Map())) mustBe Some(IncorporationStatus.rejected)
        }
      }

      "response is 2xx and JSON is valid - but the IncorpSubscriptionKey don't match those provided" must {

        "return JsResultException and log an error" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            val e = intercept[JsResultException](IncorporationInformationHttpParsers.setupSubscriptionHttpReads(regId, transactionId, subscriber, regime)
              .read("", "", HttpResponse(OK, json = incorStatusResponseJsonWrongUser, Map())))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][setupSubscriptionHttpReads] JSON returned could not be parsed to scala.Enumeration$$Value model for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }

      "response is 2xx and JSON is malformed" must {

        "return JsResultException and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[JsResultException](IncorporationInformationHttpParsers.setupSubscriptionHttpReads(regId, transactionId, subscriber, regime)
              .read("", "", HttpResponse(OK, json = Json.obj(), Map())))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][setupSubscriptionHttpReads] JSON returned could not be parsed to scala.Enumeration$$Value model for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }

      "response is ACCEPTED" must {

        "return None" in {

          IncorporationInformationHttpParsers.setupSubscriptionHttpReads(regId, transactionId, subscriber, regime)
            .read("", "", HttpResponse(ACCEPTED, "")) mustBe None
        }
      }

      "response is any other status, e.g ISE" must {

        "return IncorporationInformationResponseException and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.IncorporationInformationResponseException](IncorporationInformationHttpParsers.setupSubscriptionHttpReads(regId, transactionId, subscriber, regime)
              .read("", "/foo/bar", HttpResponse(INTERNAL_SERVER_ERROR, "")))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][setupSubscription] Calling url: '/foo/bar' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }
    }

    "calling .cancelSubscriptionHttpReads" when {

      "response is OK" must {

        "return true" in {

          IncorporationInformationHttpParsers.cancelSubscriptionHttpReads(regId, transactionId)
            .read("", "", HttpResponse(OK, "")) mustBe true
        }
      }

      "response is NOT_FOUND" must {

        "return true but log an info message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.cancelSubscriptionHttpReads(regId, transactionId)
              .read("", "", HttpResponse(NOT_FOUND, "")) mustBe true
            logs.containsMsg(Level.INFO, s"[IncorporationInformationHttpParsers][cancelSubscriptionHttpReads] no subscription found when trying to delete subscription. it might already have been deleted for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is any other status" must {

        "return false and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.cancelSubscriptionHttpReads(regId, transactionId)
              .read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")) mustBe false
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][cancelSubscriptionHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }
    }

    "calling .getCoHoCompanyDetailsHttpReads" when {

      val roAddress = Address("line 1", "line 2", None, None, Some("AA00 0AA"))
      val companyDetails = CoHoCompanyDetailsModel("Awesome Ltd", roAddress)
      val companyDetailsJson = Json.obj(
        "company_name" -> "Awesome Ltd",
        "registered_office_address" -> Json.obj(
          "address_line_1" -> "line 1",
          "address_line_2" -> "line 2",
          "postal_code" -> "AA00 0AA"
        )
      )

      "response is OK and JSON is valid" must {

        "return the IncorpInfoSuccessResponse(companyDeets)" in {

          IncorporationInformationHttpParsers.getCoHoCompanyDetailsHttpReads(regId, transactionId)
            .read("", "", HttpResponse(OK, json = companyDetailsJson, Map())) mustBe IncorpInfoSuccessResponse(companyDetails)
        }
      }

      "response is OK but the JSON is malformed" must {

        "throw JsResultException and log error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[JsResultException](IncorporationInformationHttpParsers.getCoHoCompanyDetailsHttpReads(regId, transactionId)
              .read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe IncorpInfoSuccessResponse(companyDetails))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getCoHoCompanyDetailsHttpReads] JSON returned could not be parsed to models.external.CoHoCompanyDetailsModel model for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }

      "response is BAD_REQUEST" must {

        "return IncorpInfoBadRequestResponse and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.getCoHoCompanyDetailsHttpReads(regId, transactionId)
              .read("", "", HttpResponse(BAD_REQUEST, "")) mustBe IncorpInfoBadRequestResponse
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getCoHoCompanyDetailsHttpReads] Received a BadRequest status code when expecting company details for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is NOT_FOUND" must {

        "return IncorpInfoNotFoundResponse and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.getCoHoCompanyDetailsHttpReads(regId, transactionId)
              .read("", "", HttpResponse(NOT_FOUND, "")) mustBe IncorpInfoNotFoundResponse
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getCoHoCompanyDetailsHttpReads] Received a NotFound status code when expecting company details for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is any other status" must {

        "return false and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.IncorporationInformationResponseException](IncorporationInformationHttpParsers.getCoHoCompanyDetailsHttpReads(regId, transactionId)
              .read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getCoHoCompanyDetailsHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }
    }

    "calling .getIncorpInfoDateHttpReads" when {

      val incorpDate = LocalDate.of(2020,3,1)
      val incorpDateJson = Json.obj(
        "incorporationDate" -> incorpDate.toString
      )
      val incorpDateInvalidDateJson = Json.obj(
        "incorporationDate" -> "2020-13-01"
      )

      "response is OK and JSON is valid" must {

        "return Some(incorpDate)" in {

          IncorporationInformationHttpParsers.getIncorpInfoDateHttpReads(regId, transactionId)
            .read("", "", HttpResponse(OK, json = incorpDateJson, Map())) mustBe Some(incorpDate)
        }
      }

      "response is OK but the JSON is malformed (invalid date)" must {

        "return None and log error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.getIncorpInfoDateHttpReads(regId, transactionId)
              .read("", "", HttpResponse(OK, json = incorpDateInvalidDateJson, Map())) mustBe None

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getIncorpInfoDateHttpReads] IncorpDate was retrieved from II but was not able to be parsed to LocalDate format. Value received: '2020-13-01' for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is OK but the JSON does not contain an incorpDate" must {

        "return None and log an info message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            IncorporationInformationHttpParsers.getIncorpInfoDateHttpReads(regId, transactionId)
              .read("", "", HttpResponse(OK, json = Json.obj(), Map())) mustBe None

            logs.containsMsg(Level.INFO, s"[IncorporationInformationHttpParsers][getIncorpInfoDateHttpReads] No IncorpDate was retrieved from II for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is NO_CONTENT" must {

        "return None" in {

          IncorporationInformationHttpParsers.getIncorpInfoDateHttpReads(regId, transactionId)
            .read("", "", HttpResponse(NO_CONTENT, "")) mustBe None
        }
      }

      "response is any other status" must {

        "throw Exception and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.IncorporationInformationResponseException](IncorporationInformationHttpParsers.getIncorpInfoDateHttpReads(regId, transactionId)
              .read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getIncorpInfoDateHttpReads] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }
    }

    "calling .getOfficersHttpReads" when {

      val tstOfficerListJson =
        Json.obj(
          "officers" -> Json.arr(
            Json.obj(
              "name" -> "test",
              "name_elements" -> Json.obj(
                "forename" -> "tęšt1",
                "other_forenames" -> "tèēśt11",
                "surname" -> "tæßt",
                "title" -> "Mr"
              ),
              "officer_role" -> "cic-manager"
            ),
            Json.obj(
              "name" -> "test",
              "name_elements" -> Json.obj(
                "forename" -> "žñœü",
                "other_forenames" -> "ÿürgëñ",
                "surname" -> "alfîë",
                "title" -> "Mr"
              ),
              "officer_role" -> "corporate-director"
            )
          )
        )

      val tstOfficerListModel = OfficerList(
        items = Seq(
          Officer(
            name = Name(Some("test1"), Some("teest11"), Some("tt"), Some("Mr")),
            role = "cic-manager",
            resignedOn = None,
            appointmentLink = None
          ),
          Officer(
            name = Name(Some("znu"), Some("yurgen"), Some("alfie"), Some("Mr")),
            role = "corporate-director",
            resignedOn = None,
            appointmentLink = None
          )
        )
      )

      "response is OK and JSON is valid" must {

        "return OfficerList model" in {

          IncorporationInformationHttpParsers.getOfficersHttpReads(regId, transactionId)
            .read("", "", HttpResponse(OK, json = tstOfficerListJson, Map())) mustBe tstOfficerListModel
        }
      }

      "response is OK but the JSON is malformed" must {

        "throw JsResultException and log error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[JsResultException](IncorporationInformationHttpParsers.getOfficersHttpReads(regId, transactionId)
              .read("", "", HttpResponse(OK, json = Json.obj(), Map())))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getOfficersHttpReads] JSON returned could not be parsed to models.external.OfficerList model for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }

      "response is OK but the JSON does not contain any Officers" must {

        "return OfficerListNotFoundException and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.OfficerListNotFoundException](IncorporationInformationHttpParsers.getOfficersHttpReads(regId, transactionId)
              .read("", "", HttpResponse(OK, json = Json.obj("officers" -> Json.arr()), Map())))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getOfficersHttpReads] Received an empty Officer list for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is NOT_FOUND" must {

        "return OfficerListNotFoundException and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.OfficerListNotFoundException](IncorporationInformationHttpParsers.getOfficersHttpReads(regId, transactionId)
              .read("", "", HttpResponse(NOT_FOUND, "")))

            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getOfficerList] Received a NotFound status code when expecting an Officer list for regId: $regId and txId: $transactionId")
          }
        }
      }

      "response is any other status" must {

        "throw IncorporationInformationResponseException and log an error message" in {

          withCaptureOfLoggingFrom(IncorporationInformationHttpParsers.logger) { logs =>
            intercept[DownstreamExceptions.IncorporationInformationResponseException](IncorporationInformationHttpParsers.getOfficersHttpReads(regId, transactionId)
              .read("", "", HttpResponse(INTERNAL_SERVER_ERROR, "")))
            logs.containsMsg(Level.ERROR, s"[IncorporationInformationHttpParsers][getOfficerList] Calling url: '' returned unexpected status: '$INTERNAL_SERVER_ERROR' for regId: '$regId' and txId: '$transactionId'")
          }
        }
      }
    }
  }
}
