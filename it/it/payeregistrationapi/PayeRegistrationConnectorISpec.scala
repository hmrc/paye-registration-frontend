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
package it.payeregistrationapi

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.PAYERegistrationConnector
import it.itutil.{IntegrationSpecBase, WiremockHelper}
import models.api.{CompanyDetails, Employment, FirstPayment}
import play.api.libs.json.Json
import play.api.test.FakeApplication
import uk.gov.hmrc.play.http.HeaderCarrier

class PayeRegistrationConnectorISpec extends IntegrationSpecBase {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  ))

  val regId = "12345"
  implicit val hc = HeaderCarrier()

  def url(f: String) = s"/paye-registration/$regId$f"

  "companyDetails" should {
    val validCompanyDetails = CompanyDetails(crn = None,
                                             companyName = "Test Company",
                                             tradingName = Some("Test Company Trading Name"))

    def getResponse = PAYERegistrationConnector.getCompanyDetails(regId)
    def patchResponse = PAYERegistrationConnector.upsertCompanyDetails(regId, validCompanyDetails)


    "get a model" in {
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
      stubFor(get(urlMatching(url("/company-details")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {
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
                                     firstPayment = FirstPayment(LocalDate.of(2016,1,1)))

    def getResponse = PAYERegistrationConnector.getEmployment(regId)
    def patchResponse = PAYERegistrationConnector.upsertEmployment(regId, validEmployment)

    "get a model" in {
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
      stubFor(get(urlMatching(url("/employment")))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
      )

      await(getResponse) shouldBe None
    }

    "upsert a model" in {
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