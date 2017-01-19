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

package services

import connectors._
import enums.DownstreamOutcome
import models.api.{PAYERegistration => PAYERegistrationAPI, CompanyDetails => CompanyDetailsAPI}
import models.view.{Summary, SummarySection, SummaryRow}
import fixtures.PAYERegistrationFixture
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PAYERegistrationServiceSpec extends PAYERegSpec with PAYERegistrationFixture {

  val mockRegConnector = mock[PAYERegistrationConnector]
  val mockS4LService = mock[S4LService]

  class Setup {
    val service = new PAYERegistrationService {
      override val payeRegistrationConnector = mockRegConnector
      override val s4LService = mockS4LService
      override val keystoreConnector = mockKeystoreConnector
    }
  }

  val apiRegistration = PAYERegistrationAPI(
    registrationID = "AC123456",
    formCreationTimestamp = "2017-01-11T15:10:12",
    companyDetails = CompanyDetailsAPI(
      crn = None,
      companyName = "Test Company",
      tradingName = Some("tstTrade")
    )
  )

  lazy val summary = Summary(
    Seq(SummarySection(
      id="tradingName",
      Seq(SummaryRow(
        id="tradingName",
        answer = Right("tstTrade"),
        changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
      ))
    ))
  )

  implicit val hc = HeaderCarrier()

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      await(service.assertRegistrationFootprint()) shouldBe DownstreamOutcome.Success
    }

    "return a failure response when the Registration can't be created" in new Setup {
      mockFetchRegID()
      when(mockRegConnector.createNewRegistration(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      await(service.assertRegistrationFootprint()) shouldBe DownstreamOutcome.Failure
    }
  }

  "Calling getRegistrationSummary" should {
    "return a defined Summary option when the connector returns a valid PAYE Registration response" in new Setup {

      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(apiRegistration)))

      await(service.getRegistrationSummary()) shouldBe Some(summary)
    }

    "return None when the connector returns a Forbidden response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationForbiddenResponse))

      await(service.getRegistrationSummary()) shouldBe None
    }

    "return None when the connector returns a Not Found response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationNotFoundResponse))

      await(service.getRegistrationSummary()) shouldBe None
    }

    "return None when the connector returns an exception response" in new Setup {
      mockFetchRegID("45632")
      when(mockRegConnector.getRegistration(Matchers.contains("45632"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst"))))

      await(service.getRegistrationSummary()) shouldBe None
    }
  }

  "Calling registrationToSummary" should {
    "convert a PAYE Registration API Model  to a summary model with a trading name" in new Setup {

      service.registrationToSummary(apiRegistration) shouldBe summary
    }

    "convert a PAYE Registration API Model  to a summary model without a trading name" in new Setup {


      val apiRegistrationNoTName = PAYERegistrationAPI(
        registrationID = "AC123456",
        formCreationTimestamp = "2017-01-11T15:10:12",
        companyDetails = CompanyDetailsAPI(
          crn = None,
          companyName = "Test Company",
          tradingName = None
        )
      )

      lazy val summaryNoTName = Summary(
        Seq(SummarySection(
          id="tradingName",
          Seq(SummaryRow(
            id="tradingName",
            answer = Left("noAnswerGiven"),
            changeLink = Some(controllers.userJourney.routes.CompanyDetailsController.tradingName())
          ))
        ))
      )
      service.registrationToSummary(apiRegistrationNoTName) shouldBe summaryNoTName
    }
  }

}
