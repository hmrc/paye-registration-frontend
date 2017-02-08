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
import models.api.{CompanyDetails, Employment}
import models.view.Address
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier

class PayeRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

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
    val validCompanyDetails = CompanyDetails(crn = None,
                                             companyName = "Test Company",
                                             tradingName = Some("Test Company Trading Name"),
                                             roAddress = Address(
                                               "14 St Test Walk",
                                               "Testley",
                                               Some("Testford"),
                                               Some("Testshire"),
                                               Some("TE1 1ST"), Some("UK")
                                             )
    )

    "get a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getCompanyDetails(regId)
      def patchResponse = payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)

      stubFor(get(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validCompanyDetails).toString)
        )
      )

      await(getResponse) shouldBe Some(validCompanyDetails)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getCompanyDetails(regId)
      def patchResponse = payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)

      stubFor(get(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getCompanyDetails(regId)
      def patchResponse = payeRegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)

      stubFor(patch(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validCompanyDetails).toString)
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

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getEmployment(regId)
      def patchResponse = payeRegistrationConnector.upsertEmployment(regId, validEmployment)

      stubFor(get(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validEmployment).toString)
        )
      )

      await(getResponse) shouldBe Some(validEmployment)
    }

    "get a None" in {

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getEmployment(regId)
      def patchResponse = payeRegistrationConnector.upsertEmployment(regId, validEmployment)

      stubFor(get(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {

      val payeRegistrationConnector = new PAYERegistrationConnector()

      def getResponse = payeRegistrationConnector.getEmployment(regId)
      def patchResponse = payeRegistrationConnector.upsertEmployment(regId, validEmployment)

      stubFor(patch(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(validEmployment).toString)
        )
      )

      await(patchResponse) shouldBe validEmployment
    }

  }
}