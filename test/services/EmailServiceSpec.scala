/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class EmailServiceSpec extends PayeComponentSpec {

  def service() = new EmailService {
    override val incorporationInformationConnector = mockIncorpInfoConnector
    override val payeRegistrationConnector = mockPayeRegistrationConnector
    override val s4LConnector = mockS4LConnector
    override val companyRegistrationConnector = mockCompRegConnector
    override val emailConnector = mockEmailConnector
    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  }

  "primeEmailData" should {
    "return a cache map" when {
      "the first payment date has been stashed" in {
        when(mockPayeRegistrationConnector.getEmployment(any())(any()))
          .thenReturn(Future(Some(Fixtures.validEmploymentApi)))

        when(mockS4LConnector.saveForm(any(), any(), any())(any(), any()))
          .thenReturn(Future(Fixtures.blankCacheMap))

        val result = await(service().primeEmailData("testRegId"))
        result mustBe Fixtures.blankCacheMap
      }
    }
  }

  "sendAcknowledgementEmail" should {
    "return an EmailSent" when {
      "the acknowledgement email was sent with template registerYourCompanyRegisterPAYEConfirmationNewTaxYear" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 5, 1))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailSent))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailSent
      }

      "the acknowledgement email was sent with template registerYourCompanyRegisterPAYEConfirmation" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailSent))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailSent
      }
    }

    "return EmailDifficulties" when {
      "first payment can't be fetched from S4L" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(None))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailDifficulties
      }

      "the company name couldn't be fetched from II" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any()))
          .thenReturn(Future(IncorpInfoBadRequestResponse))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailDifficulties
      }

      "there a problem sending the email" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailDifficulties))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailDifficulties
      }
    }

    "return an EmailNotFound" when {
      "No email address couldn't be fetched from CR" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any()))
          .thenReturn(Future.successful(None))

        val result = await(service().sendAcknowledgementEmail(cp, "testAckRef"))
        result mustBe EmailNotFound
      }
    }
  }
}
