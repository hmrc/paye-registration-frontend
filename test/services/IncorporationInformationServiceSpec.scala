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
import fixtures.{CoHoAPIFixture, KeystoreFixture}
import models.view.Directors
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class IncorporationInformationServiceSpec extends PAYERegSpec with KeystoreFixture with CoHoAPIFixture {

  val mockCoHoAPIConnector = mock[IncorporationInformationConnector]

  trait Setup {
    val service = new IncorporationInformationSrv {
      override val incorpInfoConnector: IncorporationInformationConnect = mockCoHoAPIConnector
      override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
    }
  }

  val tstSuccessResult = IncorpInfoSuccessResponse(validCoHoCompanyDetailsResponse)
  val tstBadRequestResult = IncorpInfoBadRequestResponse
  val tstInternalErrorResult = IncorpInfoErrorResponse(new RuntimeException)

  implicit val hc = HeaderCarrier()

  "Calling getCompanyDetails" should {
    "return the Company Details from Incorportation Information service" in new Setup {
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoSuccessResponse(validCoHoCompanyDetailsResponse)))

      await(service.getCompanyDetails("regId", "txId")) shouldBe validCoHoCompanyDetailsResponse
    }

    "throw a BadRequestException when Bad Request response is returned from Incorporation Information" in new Setup {
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoBadRequestResponse))

      a[BadRequestException] shouldBe thrownBy(await(service.getCompanyDetails("regId", "txId")))
    }

    "throw a Exception when IncorpInfoErrorResponse is returned from Incorporation Information" in new Setup {
      when(mockCoHoAPIConnector.getCoHoCompanyDetails(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(IncorpInfoErrorResponse(new Exception)))

      a[Exception] shouldBe thrownBy(await(service.getCompanyDetails("regId", "txId")))
    }
  }

  "Calling getDirectorDetails" should {
    "return the nothing when there are no directors details in the Officer list in CoHo API" in new Setup {
      when(mockCoHoAPIConnector.getOfficerList(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(invalidOfficerList))

      await(service.getDirectorDetails("testTransactionId")) shouldBe Directors(Map())
    }
    "return the directors details when there is Officer list in CoHo API" in new Setup {
      when(mockCoHoAPIConnector.getOfficerList(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validOfficerList))

      await(service.getDirectorDetails("testTransactionId")) shouldBe validDirectorDetails
    }
  }

}
