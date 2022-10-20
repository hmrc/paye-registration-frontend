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
import models.{EmailDifficulties, EmailNotFound, EmailResponse}
import models.external.{CurrentProfile, EmailRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{SystemDate, TaxYearConfig}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(companyRegistrationConnector: CompanyRegistrationConnector,
                             emailConnector: EmailConnector,
                             payeRegistrationConnector: PAYERegistrationConnector,
                             incorporationInformationConnector: IncorporationInformationConnector,
                             s4LConnector: S4LConnector,
                             taxYearConfig: TaxYearConfig
                            )(implicit val ec: ExecutionContext) {

  private val FIRST_PAYMENT_DATE = "firstPaymentDate"

  private val startDateBoolean: Boolean = SystemDate.getSystemDate.toLocalDate.isEqual(taxYearConfig.adminPeriodStart) | SystemDate.getSystemDate.toLocalDate.isAfter(taxYearConfig.adminPeriodStart)
  private val endDateBoolean: Boolean = SystemDate.getSystemDate.toLocalDate.isEqual(taxYearConfig.adminPeriodEnd) | SystemDate.getSystemDate.toLocalDate.isBefore(taxYearConfig.adminPeriodEnd)
  private val fpdEqualOrAfterNTY: LocalDate => Boolean = fpd => fpd.isEqual(taxYearConfig.taxYearStartDate) | fpd.isAfter(taxYearConfig.taxYearStartDate)

  private val templateId: LocalDate => String = fpd => if ((startDateBoolean & endDateBoolean) & fpdEqualOrAfterNTY(fpd)) {
    "register_your_company_register_paye_confirmation_new_tax_year_v2"
  } else {
    "register_your_company_register_paye_confirmation_current_tax_year_v2"
  }

  private def buildEmailRequest(verifiedEmail: String, companyName: String, ackRef: String, firstPaymentDate: LocalDate, nameFromAuth: Option[String]): EmailRequest = {
    val salutation = nameFromAuth match {
      case Some(name) => Map("salutation" -> s"Dear $name,")
      case _ => Map.empty
    }

    EmailRequest(
      to = Seq(verifiedEmail),
      templateId = templateId(firstPaymentDate),
      parameters = Map(
        "companyName" -> companyName,
        "referenceNumber" -> ackRef,
        "contactDate" -> taxYearConfig.adminPeriodEnd.format(DateTimeFormatter.ofPattern("d MMMM"))
      ) ++ salutation,
      force = false
    )
  }

  def primeEmailData(regId: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {
    for {
      Some(employment) <- payeRegistrationConnector.getEmployment(regId)
      stashed <- s4LConnector.saveForm[LocalDate](regId, FIRST_PAYMENT_DATE, employment.firstPaymentDate)
    } yield stashed
  }

  def sendAcknowledgementEmail(profile: CurrentProfile, ackRef: String, nameFromAuth: Option[String])(implicit hc: HeaderCarrier): Future[EmailResponse] =
    companyRegistrationConnector.getVerifiedEmail(profile.registrationID).flatMap {
      case Some(verifiedEmail) =>
        for {
          Some(firstPaymentDate) <- s4LConnector.fetchAndGet[LocalDate](profile.registrationID, FIRST_PAYMENT_DATE)
          iiResponse <- incorporationInformationConnector.getCoHoCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId).map { case IncorpInfoSuccessResponse(resp) => resp }
          emailRequest = buildEmailRequest(verifiedEmail, iiResponse.companyName, ackRef, firstPaymentDate, nameFromAuth)
          emailResponse <- emailConnector.requestEmailToBeSent(emailRequest)
        } yield emailResponse
      case None =>
        Future.successful(EmailNotFound)
    }.recover {
      case _ =>
        logger.warn(s"[sendAcknowledgementEmail] There was a problem sending the acknowledgement email for regId ${profile.registrationID} : txId ${profile.companyTaxRegistration.transactionId}")
        EmailDifficulties
    }

}
