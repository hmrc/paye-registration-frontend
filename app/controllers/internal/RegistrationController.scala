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

package controllers.internal

import javax.inject.{Inject, Singleton}

import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.RegistrationDeletion
import play.api.Logger
import play.api.mvc.Action
import services.{PAYERegistrationService, PAYERegistrationSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RegistrationController @Inject()(injKeystoreConnector: KeystoreConnector,
                                       injPayeRegistrationConnector: PAYERegistrationConnector,
                                       injPAYERegistrationService: PAYERegistrationService) extends RegistrationCtrl {
  val authConnector = FrontendAuthConnector
  val keystoreConnector = injKeystoreConnector
  val payeRegistrationConnector = injPayeRegistrationConnector
  val payeRegistrationService = injPAYERegistrationService
}

trait RegistrationCtrl extends FrontendController with Actions {
  val keystoreConnector: KeystoreConnect
  val payeRegistrationConnector: PAYERegistrationConnect
  val payeRegistrationService: PAYERegistrationSrv

  def delete(regId: String) = Action.async {
    implicit request => {
      val hc = HeaderCarrier.fromHeadersAndSession(request.headers)
      FrontendAuthConnector.currentAuthority(hc) flatMap {
        case Some(_) =>
          payeRegistrationService.deletePayeRegistrationInProgress(regId)(hc) map {
            case RegistrationDeletion.success => Ok
            case RegistrationDeletion.invalidStatus => PreconditionFailed
            case RegistrationDeletion.forbidden =>
              Logger.warn(s"[RegistrationController] [delete] - Requested document regId $regId to be deleted is not corresponding to the CurrentProfile regId")
              BadRequest
          } recover {
            case ex: Exception =>
              Logger.error(s"[RegistrationController] [delete] - Received an error when deleting Registration regId: $regId - error: ${ex.getMessage}")
              InternalServerError
          }
        case None =>
          Logger.warn(s"[RegistrationController] [delete] - Can't get the Authority")
          Future.successful(Unauthorized)
      } recover {
        case ex: Exception =>
          Logger.error(s"[RegistrationController] [delete] - Received an error when retrieving Authority - error: ${ex.getMessage}")
          InternalServerError
      }
    }
  }
}
