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

import javax.inject.{Inject, Singleton}

import config.FrontendAuthConnector
import enums.{AccountTypes, CacheKeys, DownstreamOutcome, RegistrationDeletion}
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import connectors._
import models.external.CurrentProfile
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYERegistrationService @Inject()(payeRegistrationConn: PAYERegistrationConnector,
                                        injKeystoreConnector: KeystoreConnector,
                                        injCurrentProfileService: CurrentProfileService,
                                        injS4LService: S4LService) extends PAYERegistrationSrv {
  override val payeRegistrationConnector = payeRegistrationConn
  override val authConnector = FrontendAuthConnector
  override val keyStoreConnector = injKeystoreConnector
  override val currentProfileService = injCurrentProfileService
  override val s4LService = injS4LService
}

trait PAYERegistrationSrv {

  val payeRegistrationConnector: PAYERegistrationConnect
  val authConnector: AuthConnector
  val keyStoreConnector: KeystoreConnect
  val currentProfileService: CurrentProfileSrv
  val s4LService: S4LSrv

  def assertRegistrationFootprint(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    payeRegistrationConnector.createNewRegistration(regId, txId)
  }

  def getAccountAffinityGroup(implicit hc: HeaderCarrier, authContext: AuthContext): Future[AccountTypes.Value] = {
    authConnector.getUserDetails[JsObject](authContext) flatMap { userDetails =>
      (userDetails \ "affinityGroup").as[String].toLowerCase match {
        case "organisation" => Future.successful(AccountTypes.Organisation)
        case _              => Future.successful(AccountTypes.InvalidAccountType)
      }
    }
  }

  def deletePayeRegistrationDocument(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    payeRegistrationConnector.deleteCurrentRegistrationDocument(regId, txId) flatMap  {
      case RegistrationDeletion.success => keyStoreConnector.remove() map {
        _ => RegistrationDeletion.success
      }
      case RegistrationDeletion.invalidStatus => Future.successful(RegistrationDeletion.invalidStatus)
    }
  }

  def deletePayeRegistrationInProgress(regId: String)(implicit hc: HeaderCarrier): Future[RegistrationDeletion.Value] = {
    getCurrentProfile flatMap { profile =>
      if( regId != profile.registrationID ) {
        Future.successful(RegistrationDeletion.forbidden)
      } else {
        s4LService.clear(regId) flatMap { response =>
          response.status match {
            case OK => payeRegistrationConnector.deleteCurrentRegistrationInProgress(regId, profile.companyTaxRegistration.transactionId)
            case INTERNAL_SERVER_ERROR => Future.successful(RegistrationDeletion.fail)
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
