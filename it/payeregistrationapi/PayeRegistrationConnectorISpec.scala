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
package payeregistrationapi

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.PAYERegistrationConnector
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.DigitalContactDetails
import models.api._
import models.Address
import models.view.PAYEContactDetails
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.MetricsService
import uk.gov.hmrc.play.http.HeaderCarrier

class PayeRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val metrics = Play.current.injector.instanceOf[MetricsService]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val regId = "12345"
  implicit val hc = HeaderCarrier()

  def url(f: String) = s"/paye-registration/$regId$f"

  "companyDetails" should {
    val validBusinessContactDetails =
      DigitalContactDetails(
        Some("test@email.com"),
        Some("1234567890"),
        Some("0987654321")
      )

    val validCompanyDetails = CompanyDetails(companyName = "Test Company",
                                             tradingName = Some("Test Company Trading Name"),
                                             roAddress = Address(
                                               "14 St Test Walk",
                                               "Testley",
                                               Some("Testford"),
                                               Some("Testshire"),
                                               Some("TE1 1ST"), Some("UK")
                                             ),
                                             ppobAddress = Address(
                                               "15 St Test Avenue",
                                               "Testpool",
                                               Some("TestUponAvon"),
                                               Some("Nowhereshire"),
                                               Some("LE1 1ST"),
                                               Some("UK")),
                                             businessContactDetails = validBusinessContactDetails
    )

    "get a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getCompanyDetails(regId)

      stubFor(get(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validCompanyDetails).toString())
        )
      )

      await(getResponse) shouldBe Some(validCompanyDetails)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getCompanyDetails(regId)

      stubFor(get(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def patchResponse = payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)

      stubFor(patch(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validCompanyDetails).toString())
        )
      )

      await(patchResponse) shouldBe validCompanyDetails
    }
  }

  "Employment" should {
    val validEmployment = Employment(employees = false,
                                     companyPension = None,
                                     subcontractors = true,
                                     firstPayDate = LocalDate.of(2016,1,1))


    "get a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getEmployment(regId)

      stubFor(get(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validEmployment).toString())
        )
      )

      await(getResponse) shouldBe Some(validEmployment)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getEmployment(regId)

      stubFor(get(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def patchResponse = payeRegistrationConnector.upsertEmployment(regId, validEmployment)

      stubFor(patch(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validEmployment).toString())
        )
      )

      await(patchResponse) shouldBe validEmployment
    }

  }

  "Director Details" should {
    val director1 = Director(
      name = Name(
        forename = Some("Fourname"),
        otherForenames = None,
        surname = "Sirname",
        title = Some("Ms")
      ),
      nino = Some("nino")
    )
    val director2 = Director(
      name = Name(
        forename = Some("FirstName"),
        otherForenames = Some("MiddleName"),
        surname = "LastName",
        title = Some("Mrs")
      ),
      nino = Some("nino2")
    )
    val dirList = Seq(director1, director2)


    "get a list of Director models" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getDirectors(regId)

      stubFor(get(urlMatching(url("/directors")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(dirList).toString())
        )
      )

      await(getResponse) shouldBe dirList
    }

    "get an empty list if no directors" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getDirectors(regId)

      stubFor(get(urlMatching(url("/directors")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe List.empty
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def patchResponse = payeRegistrationConnector.upsertDirectors(regId, dirList)

      stubFor(patch(urlMatching(url("/directors")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(dirList).toString())
        )
      )

      await(patchResponse) shouldBe dirList
    }

  }

  "SICCodes" should {
    val sicCode1 = SICCode(
      code = None,
      description = Some("laundring")
    )
    val sicCode2 = SICCode(
      code = Some("1234"),
      description = Some("consulting")
    )
    val sicCodes = Seq(sicCode1, sicCode2)


    "get a list of SICCode models" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getSICCodes(regId)

      stubFor(get(urlMatching(url("/sic-codes")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(sicCodes).toString())
        )
      )

      await(getResponse) shouldBe sicCodes
    }

    "get an empty list if no sic codes" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getSICCodes(regId)

      stubFor(get(urlMatching(url("/sic-codes")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe List.empty
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def patchResponse = payeRegistrationConnector.upsertSICCodes(regId, sicCodes)

      stubFor(patch(urlMatching(url("/sic-codes")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(sicCodes).toString())
        )
      )

      await(patchResponse) shouldBe sicCodes
    }

  }

  "PAYEContact" should {
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

    "get a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getPAYEContact(regId)

      stubFor(get(urlMatching(url("/contact-correspond-paye")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validPAYEContact).toString())
        )
      )

      await(getResponse) shouldBe Some(validPAYEContact)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getPAYEContact(regId)

      stubFor(get(urlMatching(url("/contact-correspond-paye")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def patchResponse = payeRegistrationConnector.upsertPAYEContact(regId, validPAYEContact)

      stubFor(patch(urlMatching(url("/contact-correspond-paye")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validPAYEContact).toString())
        )
      )

      await(patchResponse) shouldBe validPAYEContact
    }
  }

  "Completion Capacity" should {

    "get a string" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)
      val jobTitle = "High Priestess"

      def getResponse = payeRegistrationConnector.getCompletionCapacity(regId)

      stubFor(get(urlMatching(url("/capacity")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(jobTitle).toString())
        )
      )

      await(getResponse) shouldBe Some(jobTitle)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getCompletionCapacity(regId)

      stubFor(get(urlMatching(url("/capacity")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      val jobTitle = "High Priestess"
      def patchResponse = payeRegistrationConnector.upsertCompletionCapacity(regId, jobTitle)

      stubFor(patch(urlMatching(url("/capacity")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(jobTitle).toString())
        )
      )

      await(patchResponse) shouldBe jobTitle
    }
  }

  "Acknowledgement Reference" should {

    "get a string" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)
      val jobTitle = "High Priestess"

      def getResponse = payeRegistrationConnector.getAcknowledgementReference(regId)

      stubFor(get(urlMatching(url("/acknowledgement-reference")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(jobTitle).toString())
        )
      )

      await(getResponse) shouldBe Some(jobTitle)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector(metrics)

      def getResponse = payeRegistrationConnector.getAcknowledgementReference(regId)

      stubFor(get(urlMatching(url("/acknowledgement-reference")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }
  }
}