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

import java.time.LocalDateTime

import common.exceptions.DownstreamExceptions
import enums.DownstreamOutcome
import connectors._
import helpers.DateHelper
import models.dataModels.PAYERegistration
import models.dataModels.companyDetails.CompanyDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NoStackTrace

object PAYERegistrationService extends PAYERegistrationService {
  //$COVERAGE-OFF$
  override val keystoreConnector = KeystoreConnector
  override val payeRegistrationConnector = PAYERegistrationConnector
  override val s4LService = S4LService
  //$COVERAGE-ON$
}

sealed class NotFoundResponse extends NoStackTrace

trait PAYERegistrationService extends CommonService {

  val payeRegistrationConnector: PAYERegistrationConnector
  val s4LService: S4LService

  def fetchAndStoreCurrentRegistration()(implicit hc: HeaderCarrier): Future[Option[PAYERegistration]] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegistrationConnector.getCurrentRegistration(regID)
      outcome <- checkAndStoreCurrentRegistration(regResponse)
    } yield {
      outcome
    }
  }

  private def checkAndStoreCurrentRegistration(regResponse: PAYERegistrationResponse)(implicit hc: HeaderCarrier): Future[Option[PAYERegistration]] = {
    regResponse match {
      case PAYERegistrationSuccessResponse(reg) => s4LService.saveRegistration(reg).map {
        savedRegistration => Some(reg)
      }
      case PAYERegistrationNotFoundResponse => Future.successful(None)
      case PAYERegistrationBadRequestResponse => throw new DownstreamExceptions.PAYEMicroserviceException("Bad Request")
      case PAYERegistrationForbiddenResponse => throw new DownstreamExceptions.PAYEMicroserviceException("Un-Authorised")
      case PAYERegistrationErrorResponse(e) =>
        throw new DownstreamExceptions.PAYEMicroserviceException(s"PAYE Microservice returned exception '${e.getMessage} when fetching current registration")
    }
  }

  def createNewRegistration()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID <- fetchRegistrationID
      regResponse <- payeRegistrationConnector.createNewRegistration(regID)
    } yield regResponse match {
      case PAYERegistrationSuccessResponse(reg: PAYERegistration) => DownstreamOutcome.Success
      case _ => DownstreamOutcome.Failure
    }
  }

  def addTestRegistration(companyDetails: CompanyDetails)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    val currentDT = DateHelper.formatTimestamp(LocalDateTime.now)
    for {
      regID <- fetchRegistrationID
      outcome <- addTestRegistration(PAYERegistration(regID, currentDT, Some(companyDetails)))
    } yield outcome
  }

  def addTestRegistration(reg: PAYERegistration)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regResponse <- payeRegistrationConnector.addTestRegistration(reg)
    } yield regResponse match {
      case PAYERegistrationSuccessResponse(reg: PAYERegistration) => DownstreamOutcome.Success
      case _ => DownstreamOutcome.Failure
    }
  }

}