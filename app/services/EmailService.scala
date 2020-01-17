/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import connectors._
import javax.inject.Inject
import models.external.{CurrentProfile, EmailRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.{NewTaxYear, PAYEFeatureSwitches, SystemDate}

import scala.concurrent.Future

class EmailServiceImpl @Inject()(val companyRegistrationConnector: CompanyRegistrationConnector,
                                 val emailConnector: EmailConnector,
                                 val payeRegistrationConnector: PAYERegistrationConnector,
                                 val incorporationInformationConnector: IncorporationInformationConnector,
                                 val s4LConnector: S4LConnector,
                                 val pAYEFeatureSwitches: PAYEFeatureSwitches) extends EmailService

trait EmailService {
  val companyRegistrationConnector: CompanyRegistrationConnector
  val payeRegistrationConnector: PAYERegistrationConnector
  val emailConnector: EmailConnector
  val s4LConnector: S4LConnector
  val incorporationInformationConnector: IncorporationInformationConnector

  private val FIRST_PAYMENT_DATE = "firstPaymentDate"

  private val startDateBoolean: Boolean = SystemDate.getSystemDate.toLocalDate.isEqual(NewTaxYear.startPeriod) | SystemDate.getSystemDate.toLocalDate.isAfter(NewTaxYear.startPeriod)
  private val endDateBoolean: Boolean = SystemDate.getSystemDate.toLocalDate.isEqual(NewTaxYear.endPeriod) | SystemDate.getSystemDate.toLocalDate.isBefore(NewTaxYear.endPeriod)
  private val fpdEqualOrAfterNTY: LocalDate => Boolean = fpd => fpd.isEqual(NewTaxYear.taxYearStart) | fpd.isAfter(NewTaxYear.taxYearStart)

  private val templateId: LocalDate => String = fpd => if((startDateBoolean & endDateBoolean) & fpdEqualOrAfterNTY(fpd)) {
    "register_your_company_register_paye_confirmation_new_tax_year"
  } else {
    "register_your_company_register_paye_confirmation_current_tax_year"
  }

  private def emailRequest(verifiedEmail: String, companyName: String, ackRef: String, firstPaymentDate: LocalDate): EmailRequest = EmailRequest(
    to = Seq(verifiedEmail),
    templateId = templateId(firstPaymentDate),
    parameters = Map(
      "companyName"     -> companyName,
      "referenceNumber" -> ackRef
    ),
    force = false
  )

  def primeEmailData(regId: String)(implicit hc: HeaderCarrier): Future[CacheMap] = {
      for {
        Some(employment) <- payeRegistrationConnector.getEmployment(regId)
        stashed          <- s4LConnector.saveForm[LocalDate](regId, FIRST_PAYMENT_DATE, employment.firstPaymentDate)
      } yield stashed
  }

  def sendAcknowledgementEmail(profile: CurrentProfile, ackRef: String)(implicit hc: HeaderCarrier): Future[EmailResponse] = {
    companyRegistrationConnector.getVerifiedEmail(profile.registrationID) flatMap {
      _.fold[Future[EmailResponse]](Future(EmailNotFound)) { verifiedEmail =>
        (for {
          Some(firstPaymentDate) <- s4LConnector.fetchAndGet[LocalDate](profile.registrationID, FIRST_PAYMENT_DATE)
          iiResponse             <- incorporationInformationConnector.getCoHoCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map {
            case IncorpInfoSuccessResponse(resp) => resp
          }
          emailResponse          <- emailConnector.requestEmailToBeSent(emailRequest(
            verifiedEmail    = verifiedEmail,
            companyName      = iiResponse.companyName,
            ackRef           = ackRef,
            firstPaymentDate = firstPaymentDate
          ))
        } yield emailResponse).recover {
          case _ =>
            logger.warn(s"[sendAcknowledgementEmail] - There was a problem sending the acknowledgement email for regId ${profile.registrationID} : txId ${profile.companyTaxRegistration.transactionId}")
            EmailDifficulties
        }
      }
    }
  }
}
