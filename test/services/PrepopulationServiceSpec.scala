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

import connectors.BusinessRegistrationConnect
import models.DigitalContactDetails
import models.view.PAYEContactDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import testHelpers.PAYERegSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class PrepopulationServiceSpec extends PAYERegSpec {

  trait Setup {
    val service = new PrepopulationSrv {
      override val busRegConnector: BusinessRegistrationConnect = mockBusinessRegistrationConnector
    }
  }

  implicit val hc = new HeaderCarrier()
  val regId = "55555"

  val validDigitalContact = DigitalContactDetails(Some("a@b.c"), Some("123"), Some("321"))
  val validContactDetails = PAYEContactDetails("testName", validDigitalContact)

  "getBusinessContactDetails" should {
    "return optional digital contact details" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.eq(hc), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validContactDetails)))

      await(service.getBusinessContactDetails(regId)) shouldBe Some(validDigitalContact)
    }
    "return no digital contact details" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.eq(hc), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      await(service.getBusinessContactDetails(regId)) shouldBe None
    }
  }

  "getPAYEContactDetails" should {
    "return optional contact details" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.eq(hc), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(validContactDetails)))

      await(service.getPAYEContactDetails(regId)) shouldBe Some(validContactDetails)
    }
    "return no contact details" in new Setup {
      when(mockBusinessRegistrationConnector.retrieveContactDetails(ArgumentMatchers.eq(regId))(ArgumentMatchers.eq(hc), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      await(service.getPAYEContactDetails(regId)) shouldBe None
    }
  }

  "saveContactDetails" should {
    "save contact details" in new Setup {
      when(mockBusinessRegistrationConnector.upsertContactDetails(ArgumentMatchers.eq(regId), ArgumentMatchers.any[PAYEContactDetails])(ArgumentMatchers.eq(hc)))
        .thenReturn(Future.successful(validContactDetails))

      await(service.saveContactDetails(regId, validContactDetails)) shouldBe validContactDetails
    }
  }
}
