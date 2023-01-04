/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import connectors._
import helpers.PayeComponentSpec
import models.{EmailDifficulties, EmailNotFound, EmailSent}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.test.FakeRequest
import utils.{PAYEFeatureSwitch, TaxYearConfig}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class EmailServiceSpec extends PayeComponentSpec {

  implicit val request: FakeRequest[_] = FakeRequest()

  object TestAppConfig extends AppConfig(mock[Configuration], mock[PAYEFeatureSwitch]) {
    override lazy val adminPeriodStart: String = "2022-02-06"
    override lazy val adminPeriodEnd: String = "2022-05-17"
    override lazy val taxYearStartDate: String = "2022-04-06"
  }

  val service = new EmailService(
    mockCompRegConnector,
    mockEmailConnector,
    mockPayeRegistrationConnector,
    mockIncorpInfoConnector,
    mockS4LConnector,
    new TaxYearConfig(TestAppConfig)
  )(ExecutionContext.Implicits.global)


  "primeEmailData" should {
    "return a cache map" when {
      "the first payment date has been stashed" in {
        when(mockPayeRegistrationConnector.getEmployment(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future(Some(Fixtures.validEmploymentApi)))

        when(mockS4LConnector.saveForm(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(Fixtures.blankCacheMap))

        val result = await(service.primeEmailData("testRegId"))
        result mustBe Fixtures.blankCacheMap
      }
    }
  }

  "sendAcknowledgementEmail" should {
    "return an EmailSent" when {
      "the acknowledgement email was sent with template registerYourCompanyRegisterPAYEConfirmationNewTaxYear" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 5, 1))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any(), any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailSent))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailSent
      }

      "the acknowledgement email was sent with template registerYourCompanyRegisterPAYEConfirmation" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any(), any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailSent))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailSent
      }
    }

    "return EmailDifficulties" when {
      "first payment can't be fetched from S4L" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(None))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailDifficulties
      }

      "the company name couldn't be fetched from II" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any(), any()))
          .thenReturn(Future(IncorpInfoBadRequestResponse))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailDifficulties
      }

      "there a problem sending the email" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("foo@foo.com")))

        when(mockS4LConnector.fetchAndGet[LocalDate](any(), any())(any(), any()))
          .thenReturn(Future(Some(LocalDate.of(2018, 10, 26))))

        when(mockIncorpInfoConnector.getCoHoCompanyDetails(any(), any())(any(), any()))
          .thenReturn(Future(IncorpInfoSuccessResponse(Fixtures.validCoHoCompanyDetailsResponse)))

        when(mockEmailConnector.requestEmailToBeSent(any())(any()))
          .thenReturn(Future.successful(EmailDifficulties))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailDifficulties
      }
    }

    "return an EmailNotFound" when {
      "No email address couldn't be fetched from CR" in {
        when(mockCompRegConnector.getVerifiedEmail(any())(any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result = await(service.sendAcknowledgementEmail(cp, "testAckRef", Some("Name from auth")))
        result mustBe EmailNotFound
      }
    }
  }
}
