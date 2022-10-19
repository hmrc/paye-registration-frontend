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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.api._
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class PayeRegistrationConnectorISpec extends IntegrationSpecBase {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map(
      "microservice.services.paye-registration.port" -> s"${WiremockHelper.wiremockPort}",
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
    ))
    .build

  val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnectorImpl]

  val regId = "12345"
  val txnId = "67890"

  implicit val hc = HeaderCarrier()

  def url(f: String) = s"/paye-registration/$regId$f"

  "For Company Details" when {

    val validBusinessContactDetails =
      DigitalContactDetails(
        email = Some("test@email.com"),
        mobileNumber = Some("1234567890"),
        phoneNumber = Some("0987654321")
      )

    val validCompanyDetails =
      CompanyDetails(
        companyName = "Test Company",
        tradingName = Some("Test Company Trading Name"),
        roAddress = Address(
          line1 = "14 St Test Walk",
          line2 = "Testley",
          line3 = Some("Testford"),
          line4 = Some("Testshire"),
          postCode = Some("TE1 1ST"), country = Some("UK")
        ),
        ppobAddress = Address(
          line1 = "15 St Test Avenue",
          line2 = "Testpool",
          line3 = Some("TestUponAvon"),
          line4 = Some("Nowhereshire"),
          postCode = Some("LE1 1ST"),
          country = Some("UK")),
        businessContactDetails = validBusinessContactDetails
      )

    val validCompanyDetailsJson = Json.toJson(validCompanyDetails)

    "calling .getCompanyDetails(regId: String)" must {

      def stubGetCompanyDetails(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/company-details"))).willReturn(buildResponse(status, body)))

      "return the Company Details for a success response" in {

        stubGetCompanyDetails(OK, Some(validCompanyDetailsJson))

        await(payeRegistrationConnector.getCompanyDetails(regId)) mustBe Some(validCompanyDetails)
      }

      "return None when NOT_FOUND response" in {

        stubGetCompanyDetails(NOT_FOUND, None)

        await(payeRegistrationConnector.getCompanyDetails(regId)) mustBe None
      }

      "throw Exception when INTERNAL_SERVER_ERROR response" in {

        stubGetCompanyDetails(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getCompanyDetails(regId)))
      }
    }

    "calling .upsertCompanyDetails(regId: String, companyDetails: CompanyDetails)" must {

      def stubUpsertCompanyDetails(companyDetails: JsValue)(status: Int, body: Option[JsValue]) =
        stubFor(
          patch(urlMatching(url("/company-details")))
            .withRequestBody(equalToJson(companyDetails.toString))
            .willReturn(buildResponse(status, body))
        )

      "return the upserted Company Details on a success response" in {

        stubUpsertCompanyDetails(validCompanyDetailsJson)(OK, Some(validCompanyDetailsJson))

        await(payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)) mustBe validCompanyDetails
      }

      "return Exception when NOT_FOUND response" in {

        stubUpsertCompanyDetails(validCompanyDetailsJson)(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)))
      }

      "throw Exception when INTERNAL_SERVER_ERROR response" in {

        stubUpsertCompanyDetails(validCompanyDetailsJson)(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)))
      }
    }
  }

  "For Director Details" when {

    val director1 = Director(
      name = Name(
        forename = Some("Fourname"),
        otherForenames = None,
        surname = Some("Sirname"),
        title = Some("Ms")
      ),
      nino = Some("nino")
    )
    val director2 = Director(
      name = Name(
        forename = Some("FirstName"),
        otherForenames = Some("MiddleName"),
        surname = Some("LastName"),
        title = Some("Mrs")
      ),
      nino = Some("nino2")
    )

    val dirList = Seq(director1, director2)

    "calling .getDirectors(regId: String)" must {

      def stubGetDirectors(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/directors"))).willReturn(buildResponse(status, body)))

      "return a list of Director models when success response" in {

        stubGetDirectors(OK, Some(Json.toJson(dirList)))

        await(payeRegistrationConnector.getDirectors(regId)) mustBe dirList
      }

      "return an empty list when NOT_FOUND response" in {

        stubGetDirectors(NOT_FOUND, None)

        await(payeRegistrationConnector.getDirectors(regId)) mustBe List.empty
      }

      "throw an Exception for any other response" in {

        stubGetDirectors(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getDirectors(regId)))
      }
    }

    "calling .upsertDirectors(regId: String, directors: Seq[Director])" must {

      def stubUpsertDirectors(directors: JsValue)(status: Int, body: Option[JsValue]) =
        stubFor(
          patch(urlMatching(url("/directors")))
            .withRequestBody(equalToJson(directors.toString))
            .willReturn(buildResponse(status, body))
        )

      "return the upserted Directors on a success response" in {

        stubUpsertDirectors(Json.toJson(dirList))(OK, Some(Json.toJson(dirList)))

        await(payeRegistrationConnector.upsertDirectors(regId, dirList)) mustBe dirList
      }

      "return Exception when NOT_FOUND response" in {

        stubUpsertDirectors(Json.toJson(dirList))(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.upsertDirectors(regId, dirList)))
      }

      "throw Exception when INTERNAL_SERVER_ERROR response" in {

        stubUpsertDirectors(Json.toJson(dirList))(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.upsertDirectors(regId, dirList)))
      }
    }
  }

  "For SICCodes" should {
    val sicCode1 = SICCode(
      code = None,
      description = Some("laundring")
    )
    val sicCode2 = SICCode(
      code = Some("1234"),
      description = Some("consulting")
    )
    val sicCodes = Seq(sicCode1, sicCode2)

    "calling .getSICCodes(regId: String)" must {

      def stubGetSICCodes(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/sic-codes"))).willReturn(buildResponse(status, body)))

      "return a list of SICCode models when a success response" in {

        stubGetSICCodes(OK, Some(Json.toJson(sicCodes)))

        await(payeRegistrationConnector.getSICCodes(regId)) mustBe sicCodes
      }

      "return an empty list if NOT_FOUND response" in {

        stubGetSICCodes(NOT_FOUND, None)

        await(payeRegistrationConnector.getSICCodes(regId)) mustBe List.empty
      }
    }

    "calling .upsertSICCodes(regId: String, sicCodes: Seq[SicCode])" must {

      def stubUpsertSICCodes(sicCodes: JsValue)(status: Int, body: Option[JsValue]) =
        stubFor(
          patch(urlMatching(url("/sic-codes")))
            .withRequestBody(equalToJson(sicCodes.toString))
            .willReturn(buildResponse(status, body))
        )

      "return the upserted model when success response" in {

        stubUpsertSICCodes(Json.toJson(sicCodes))(OK, Some(Json.toJson(sicCodes)))

        await(payeRegistrationConnector.upsertSICCodes(regId, sicCodes)) mustBe sicCodes
      }

      "throw an Exception for any other response" in {

        stubUpsertSICCodes(Json.toJson(sicCodes))(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.upsertSICCodes(regId, sicCodes)))
      }
    }
  }

  "For PAYEContact" should {

    val validPAYEContact = PAYEContact(
      contactDetails = PAYEContactDetails(
        name = "Thierry Henry",
        digitalContactDetails = DigitalContactDetails(
          Some("testy@tasty.com"),
          Some("1234"),
          Some("9874578")
        )
      ),
      correspondenceAddress = Address(
        line1 = "tst1",
        line2 = "tst2",
        line3 = None,
        line4 = None,
        postCode = Some("tstCode")
      )
    )

    "calling .getPAYEContact" must {

      def stubGetPAYEContact(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/contact-correspond-paye"))).willReturn(buildResponse(status, body)))

      "return PAYEContact model for success response" in {

        stubGetPAYEContact(OK, Some(Json.toJson(validPAYEContact)))

        await(payeRegistrationConnector.getPAYEContact(regId)) mustBe Some(validPAYEContact)
      }

      "return None if NOT_FOUND response" in {

        stubGetPAYEContact(NOT_FOUND, None)

        await(payeRegistrationConnector.getPAYEContact(regId)) mustBe None
      }

      "return an Exception for any other response" in {

        stubGetPAYEContact(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getPAYEContact(regId)))
      }
    }

    "calling .upsertPAYEContact(regId: String, payeContact: PAYEContact)" must {

      def stubUpsertPAYEContact(payeContact: JsValue)(status: Int, body: Option[JsValue]) =
        stubFor(
          patch(urlMatching(url("/contact-correspond-paye")))
            .withRequestBody(equalToJson(payeContact.toString))
            .willReturn(buildResponse(status, body))
        )

      "return the upserted model when success response" in {

        stubUpsertPAYEContact(Json.toJson(validPAYEContact))(OK, Some(Json.toJson(validPAYEContact)))

        await(payeRegistrationConnector.upsertPAYEContact(regId, validPAYEContact)) mustBe validPAYEContact
      }

      "throw an Exception for any other response" in {

        stubUpsertPAYEContact(Json.toJson(validPAYEContact))(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.upsertPAYEContact(regId, validPAYEContact)))
      }
    }
  }

  "For Completion Capacity" when {

    val jobTitle = "High Priestess"

    "calling .getCompletionCapacity(regId: String)" must {

      def stubGetCompletionCapacity(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/capacity"))).willReturn(buildResponse(status, body)))

      "return the JobTile for a success response" in {

        stubGetCompletionCapacity(OK, Some(JsString(jobTitle)))

        await(payeRegistrationConnector.getCompletionCapacity(regId)) mustBe Some(jobTitle)
      }

      "return a None if NOT_FOUND response" in {

        stubGetCompletionCapacity(NOT_FOUND, None)

        await(payeRegistrationConnector.getCompletionCapacity(regId)) mustBe None
      }

      "return an Exception for any other response" in {

        stubGetCompletionCapacity(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getCompletionCapacity(regId)))
      }
    }

    "calling .upsertCompletionCapacity(regId: String, jobTitle: String)" must {

      def stubUpsertCompletionCapacity(jobTitle: JsValue)(status: Int, body: Option[JsValue]) =
        stubFor(
          patch(urlMatching(url("/capacity")))
            .withRequestBody(equalToJson(jobTitle.toString))
            .willReturn(buildResponse(status, body))
        )

      "return the upserted model on success" in {

        stubUpsertCompletionCapacity(JsString(jobTitle))(OK, Some(JsString(jobTitle)))

        await(payeRegistrationConnector.upsertCompletionCapacity(regId, jobTitle)) mustBe jobTitle
      }

      "throw an Exception for any other response" in {

        stubUpsertCompletionCapacity(JsString(jobTitle))(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.upsertCompletionCapacity(regId, jobTitle)))
      }
    }
  }

  "For Acknowledgement Reference" when {

    val ackRef = "ackRef12345"

    "calling .getAcknowledgementReference(regId: String)" must {

      def stubGetAcknowledgementReference(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(url("/acknowledgement-reference"))).willReturn(buildResponse(status, body)))

      "return the AcknowledgementReference on success" in {

        stubGetAcknowledgementReference(OK, Some(JsString(ackRef)))

        await(payeRegistrationConnector.getAcknowledgementReference(regId)) mustBe Some(ackRef)
      }

      "return a None for NOT_FOUND response" in {

        stubGetAcknowledgementReference(NOT_FOUND, None)

        await(payeRegistrationConnector.getAcknowledgementReference(regId)) mustBe None
      }

      "throw and Exception for any other response" in {

        stubGetAcknowledgementReference(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getAcknowledgementReference(regId)))
      }
    }
  }

  "For Registration ID" when {

    "calling .getRegistrationId(txnId: String)" must {

      def stubGetRegistrationId(status: Int, body: Option[JsValue]) =
        stubFor(get(urlMatching(s"/paye-registration/$txnId/registration-id")).willReturn(buildResponse(status, body)))

      "return RegId for a success response" in {

        stubGetRegistrationId(OK, Some(JsString(regId)))

        await(payeRegistrationConnector.getRegistrationId(txnId)) mustBe regId
      }

      "return an exception when not found" in {

        stubGetRegistrationId(NOT_FOUND, None)

        intercept[Exception](await(payeRegistrationConnector.getRegistrationId(txnId))).getMessage mustBe
          "GET of 'http://localhost:11111/paye-registration/67890/registration-id' returned 404 (Not Found). Response body: ''"
      }

      "return an exception when any other unexpected status returned" in {

        stubGetRegistrationId(INTERNAL_SERVER_ERROR, None)

        intercept[Exception](await(payeRegistrationConnector.getRegistrationId(txnId))).getMessage mustBe
          "GET of 'http://localhost:11111/paye-registration/67890/registration-id' returned 500. Response body: ''"
      }
    }
  }
}