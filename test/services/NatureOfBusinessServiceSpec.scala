/*
 * Copyright 2019 HM Revenue & Customs
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

import enums.DownstreamOutcome
import helpers.PayeComponentSpec
import models.api.SICCode
import models.view.NatureOfBusiness
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class NatureOfBusinessServiceSpec extends PayeComponentSpec {
  val returnHttpResponse = HttpResponse(200)

  class Setup {
    val service = new NatureOfBusinessService {
      override val payeRegConnector = mockPAYERegConnector
    }
  }

  "Calling sicCodes2NatureOfBusiness" should {
    "correctly produce a NatureOfBusiness view model from a list of SICCode API model" in new Setup {
      val tstModelAPI = Seq(
        SICCode(
          code = Some("999"),
          description = Some("banking")
        ),
        SICCode(
          code = Some("1234"),
          description = Some("construction")
        )
      )

      val tstModelView = NatureOfBusiness(natureOfBusiness = "banking")

      service.sicCodes2NatureOfBusiness(tstModelAPI) mustBe Some(tstModelView)
    }

    "produce an empty model from an empty list of SICCode API model" in new Setup {
      val tstModelAPI = Seq.empty

      service.sicCodes2NatureOfBusiness(tstModelAPI) mustBe None
    }
  }

  "Calling natureOfBusiness2SICCodes" should {
    "correctly produce a list of SICCode API model from a NatureOfBusiness view model" in new Setup {
      val tstModelAPI = Seq(
        SICCode(
          code = None,
          description = Some("laundring")
        )
      )

      val tstModelView = NatureOfBusiness(natureOfBusiness = "laundring")

      service.natureOfBusiness2SICCodes(tstModelView) mustBe tstModelAPI
    }
  }

  "Calling getNatureOfBusiness" should {
    "return the correct View response when SIC Codes are returned from the microservice" in new Setup {
      when(mockPAYERegConnector.getSICCodes(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validSICCodesList))

      await(service.getNatureOfBusiness("54321")) mustBe Some(NatureOfBusiness(natureOfBusiness = "laundring"))
    }

    "throw an Upstream4xxResponse when a 403 response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getSICCodes(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(Upstream4xxResponse("403", 403, 403)))

      an[Upstream4xxResponse] mustBe thrownBy(await(service.getNatureOfBusiness("54321")))
    }

    "throw an Exception when `an unexpected response is returned from the connector" in new Setup {
      when(mockPAYERegConnector.getSICCodes(ArgumentMatchers.contains("54321"))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ArrayIndexOutOfBoundsException))

      an[Exception] mustBe thrownBy(await(service.getNatureOfBusiness("54321")))
    }
  }

  "Calling saveNatureOfBusiness" should {
    "return a success response when the upsert completes successfully" in new Setup {
      val validNatureOfBusiness = NatureOfBusiness(natureOfBusiness = "laundring")

      when(mockPAYERegConnector.upsertSICCodes(ArgumentMatchers.contains("54321"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Fixtures.validSICCodesList))

      await(service.saveNatureOfBusiness(validNatureOfBusiness, "54321")) mustBe DownstreamOutcome.Success
    }
  }
}
