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

  "Caling getTradingName" should {
    "return a defined Trading Name View option if returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validCompanyDetailsAPI)))

      await(service.getTradingName()) shouldBe Some(validTradingNameViewModel)
    }

    "return a defined, negative Trading Name View option if no Trading Name details are returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationSuccessResponse(validCompanyDetailsAPI.copy(tradingName = None))))

      await(service.getTradingName()) shouldBe Some(negativeTradingNameViewModel)
    }

    "return an empty option if no Company Details are returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationNotFoundResponse))

      await(service.getTradingName()) shouldBe None
    }

    "throw a PAYEMicroserviceException an exception response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationErrorResponse(new RuntimeException("tst"))))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.getTradingName()))
    }

    "throw a PAYEMicroserviceException an unexpected response is returned from the connector" in new Setup {
      mockFetchRegID("54321")
      when(mockPAYERegConnector.getCompanyDetails(Matchers.contains("54321"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(PAYERegistrationForbiddenResponse))

      a[PAYEMicroserviceException] shouldBe thrownBy(await(service.getTradingName()))
    }

  }
}
