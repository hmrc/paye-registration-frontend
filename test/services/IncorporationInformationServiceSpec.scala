/*
 * Copyright 2018 HM Revenue & Customs
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
import helpers.PayeComponentSpec
import models.view.Directors
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.Future

class IncorporationInformationServiceSpec extends PayeComponentSpec {
  class Setup {
    val service = new IncorporationInformationService {
      override val incorpInfoConnector = mockIncorpInfoConnector
      override val keystoreConnector   = mockKeystoreConnector
    }
  }

  val tstSuccessResult = IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)
  val tstBadRequestResult = IncorpInfoBadRequestResponse
  val tstInternalErrorResult = IncorpInfoErrorResponse(new RuntimeException)


  "Calling getCompanyDetails" should {
    "return the Company Details from Incorportation Information service" in new Setup {
      when(mockIncorpInfoConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

      await(service.getCompanyDetails("regId", "txId")) mustBe Fixtures.validCoHoCompanyDetailsResponse
    }

    "throw a BadRequestException when Bad Request response is returned from Incorporation Information" in new Setup {
      when(mockIncorpInfoConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoBadRequestResponse))

      a[BadRequestException] mustBe thrownBy(await(service.getCompanyDetails("regId", "txId")))
    }

    "throw a Exception when IncorpInfoNotFoundResponse is returned from Incorporation Information" in new Setup {
      when(mockIncorpInfoConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoNotFoundResponse))

      a[Exception] mustBe thrownBy(await(service.getCompanyDetails("regId", "txId")))
    }

    "throw a Exception when IncorpInfoErrorResponse is returned from Incorporation Information" in new Setup {
      when(mockIncorpInfoConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoErrorResponse(new Exception)))

      a[Exception] mustBe thrownBy(await(service.getCompanyDetails("regId", "txId")))
    }
  }

  "Calling getDirectorDetails" should {
    "return the nothing when there are no directors details in the Officer list in CoHo API" in new Setup {
      when(mockIncorpInfoConnector.getOfficerList(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(Fixtures.invalidOfficerList))

      await(service.getDirectorDetails("testTransactionId","testRegId")) mustBe Directors(Map())
    }
    "return the directors details when there is Officer list in CoHo API" in new Setup {
      when(mockIncorpInfoConnector.getOfficerList(ArgumentMatchers.anyString(),ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(Fixtures.validOfficerList))

      await(service.getDirectorDetails("testTransactionId","testRegId")) mustBe Fixtures.validDirectorDetails
    }
  }
}
