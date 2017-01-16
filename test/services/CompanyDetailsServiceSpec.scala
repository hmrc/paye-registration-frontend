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
import fixtures.{PAYERegistrationFixture, S4LFixture}
import models.view.{CompanyDetails => CompanyDetailsView, TradingName => TradingNameView}
import models.api.{CompanyDetails => CompanyDetailsAPI}
import org.mockito.Matchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import common.exceptions.DownstreamExceptions.PAYEMicroserviceException
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class CompanyDetailsServiceSpec extends PAYERegSpec with S4LFixture with PAYERegistrationFixture {

  implicit val hc = HeaderCarrier()

  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  class Setup {
    val service = new CompanyDetailsService {
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector
    }
  }

  class NoCompanyDetailsMockedSetup {
    val service = new CompanyDetailsService {
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector

      override def getCompanyDetails()(implicit hc: HeaderCarrier): Future[Option[CompanyDetailsView]] = {
        Future.successful(None)
      }
    }
  }

  class CompanyDetailsMockedSetup {
    val service = new CompanyDetailsService {
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector

      override def getCompanyDetails()(implicit hc: HeaderCarrier): Future[Option[CompanyDetailsView]] = {
        Future.successful(Some(validCompanyDetailsViewModel))
      }
    }
  }

  class APIConverterMockedSetup {
    val service = new CompanyDetailsService {
      override val keystoreConnector = mockKeystoreConnector
      override val payeRegConnector = mockPAYERegConnector

      override def apiToView(apiModel: CompanyDetailsAPI): CompanyDetailsView = {
        validCompanyDetailsViewModel
      }
    }
  }

  "Calling getTradingName" should {
    "return a defined Trading Name View option if returned from the connector" in new CompanyDetailsMockedSetup {
      mockFetchRegID("54321")

      await(service.getTradingName()) shouldBe Some(validTradingNameViewModel)
    }

    "return an empty option if no Company Details are returned from the connector" in new NoCompanyDetailsMockedSetup {
      mockFetchRegID("54321")

      await(service.getTradingName()) shouldBe None
    }
  }

  "Calling apiToView" should {
    "correctly produce a view model from a Company Details API model with a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        Some("trading name")
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = true,
          tradingName = Some("trading name")
        ))
      )
      service.apiToView(tstModelAPI) shouldBe tstModelView
    }

    "correctly produce a view model from a Company Details API model without a completed trading name" in new Setup {
      val tstModelAPI = CompanyDetailsAPI(
        Some("tstCRN"),
        "Comp name",
        None
      )
      val tstModelView = CompanyDetailsView(
        Some("tstCRN"),
        "Comp name",
        Some(TradingNameView(
          differentName = false,
          tradingName = None
        ))
      )
      service.apiToView(tstModelAPI) shouldBe tstModelView
    }
  }

  "Calling getCompanyDetails" should {

    "return the correct View response when Company Details are returned from the connector" in new APIConverterMockedSetup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validCompanyDetailsAPI)))

      await(service.getCompanyDetails()) shouldBe Some(validCompanyDetailsViewModel)
    }

    "return an empty option when no Company Details are returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationNotFoundResponse))

      await(service.getCompanyDetails()) shouldBe None
    }

    "throw a PAYEMicroserviceException an exception response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst"))))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.getCompanyDetails()))
    }

    "throw a PAYEMicroserviceException an unexpected response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationForbiddenResponse))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.getCompanyDetails()))
    }

  }
}
