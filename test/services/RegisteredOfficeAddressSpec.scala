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

import connectors.{CoHoAPIConnect, CoHoAPIConnector, CompanyRegistrationConnect}
import models.external.CHROAddress
import testHelpers.PAYERegSpec
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class RegisteredOfficeAddressSpec extends PAYERegSpec {

  val mockCompRegConnector = mock[CompanyRegistrationConnect]
  val mockCohoAPIConnector = mock[CoHoAPIConnector]

  class Setup {
    val service = new RegisteredOfficeAddressSrv {
      val compRegConnector: CompanyRegistrationConnect = mockCompRegConnector
      val cohoAPIConnector: CoHoAPIConnect = mockCohoAPIConnector
    }

    implicit val hc = HeaderCarrier()
  }

  "retrieveRegisteredOfficeAddress" should {

    val testAddr =
      CHROAddress(
        "premises",
        "l1",
        Some("l2"),
        "locality",
        Some("country"),
        Some("pobox"),
        Some("pCode"),
        Some("region")
      )

    "return a transactionId" in new Setup {
      when(mockCompRegConnector.getTransactionId(Matchers.eq("testRegId"))(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("testTransId"))

      when(mockCohoAPIConnector.getRegisteredOfficeAddress(Matchers.eq("testTransId"))(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(testAddr))

      val result = await(service.retrieveRegisteredOfficeAddress("testRegId"))
      result shouldBe testAddr
    }
  }
}
