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

import java.time.LocalDate

import javax.inject.Inject
import connectors.PAYERegistrationConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.{NewTaxYear, SystemDate}

import scala.concurrent.Future

class ConfirmationServiceImpl @Inject()(val payeRegistrationConnector: PAYERegistrationConnector) extends ConfirmationService {
  def now: LocalDate  = SystemDate.getSystemDate.toLocalDate
  val startDate       = NewTaxYear.startPeriod
  val endDate         = NewTaxYear.endPeriod
}

trait ConfirmationService {
  def now: LocalDate

  val startDate: LocalDate
  val endDate: LocalDate

  val payeRegistrationConnector: PAYERegistrationConnector

  def getAcknowledgementReference(regId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    payeRegistrationConnector.getAcknowledgementReference(regId)
  }

  def determineIfInclusiveContentIsShown: Boolean = (now.isAfter(startDate) && now.isBefore(endDate)) | now.isEqual(startDate) | now.isEqual(endDate)
}
