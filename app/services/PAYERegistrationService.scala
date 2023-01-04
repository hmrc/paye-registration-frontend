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

import connectors._
import enums.{CacheKeys, DownstreamOutcome, IncorporationStatus, RegistrationDeletion}
import models.external.CurrentProfile
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PAYERegistrationServiceImpl @Inject()(val payeRegistrationConnector: PAYERegistrationConnector,
                                            val keyStoreConnector: KeystoreConnector,
                                            val currentProfileService: CurrentProfileService,
                                            val s4LService: S4LService)(implicit val ec: ExecutionContext) extends PAYERegistrationService

trait PAYERegistrationService {

  val payeRegistrationConnector: PAYERegistrationConnector
  val keyStoreConnector: KeystoreConnector
  val currentProfileService: CurrentProfileService
  val s4LService: S4LService
  implicit val ec: ExecutionContext

  def assertRegistrationFootprint(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    payeRegistrationConnector.createNewRegistration(regId, txId)
  }

  def deleteRejectedRegistration(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    payeRegistrationConnector.deleteRejectedRegistrationDocument(regId, txId) flatMap {
      case RegistrationDeletion.success | RegistrationDeletion.notfound => keyStoreConnector.remove() map {
        _ => RegistrationDeletion.success
      }
      case RegistrationDeletion.invalidStatus => Future.successful(RegistrationDeletion.invalidStatus)
    }
  }

  def handleIIResponse(txId: String, status: IncorporationStatus.Value)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    currentProfileService.updateCurrentProfileWithIncorpStatus(txId, status).flatMap { oRegIdFromCp =>
      if (status == IncorporationStatus.rejected) {
        oRegIdFromCp.fold(payeRegistrationConnector.getRegistrationId(txId))(Future.successful) flatMap { regId =>
          tearDownUserData(regId, txId)
        } recoverWith {
          case e: NotFoundException => Future.successful(RegistrationDeletion.notfound)
          case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND => Future.successful(RegistrationDeletion.notfound)
        }
      }
      else {
        Future.successful(RegistrationDeletion.invalidStatus)
      }
    }
  }

  private def tearDownUserData(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = for {
    _ <- s4LService.clear(regId)
    response <- payeRegistrationConnector.deleteRegistrationForRejectedIncorp(regId, txId)
  } yield response

  def deletePayeRegistrationInProgress(regId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    getCurrentProfile flatMap { profile =>
      if (regId != profile.registrationID) {
        Future.successful(RegistrationDeletion.forbidden)
      } else {
        s4LService.clear(regId) flatMap { response =>
          response.status match {
            case NO_CONTENT => payeRegistrationConnector.deleteCurrentRegistrationInProgress(regId, profile.companyTaxRegistration.transactionId)
          }
        }
      }
    }
  }

  private def getCurrentProfile(implicit hc: HeaderCarrier): Future[CurrentProfile] = {
    keyStoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString) flatMap {
      case Some(currentProfile) => Future.successful(currentProfile)
      case None => currentProfileService.fetchAndStoreCurrentProfile
    }
  }
}
