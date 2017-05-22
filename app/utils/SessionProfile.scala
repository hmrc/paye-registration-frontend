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

package utils

import common.exceptions.InternalExceptions
import connectors.{KeystoreConnect, PAYERegistrationConnect}
import enums.{CacheKeys, PAYEStatus}
import models.external.CurrentProfile
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SessionProfile extends InternalExceptions {

  val keystoreConnector: KeystoreConnect
  val payeRegistrationConnector: PAYERegistrationConnect

  def withCurrentProfile(f: => CurrentProfile => Future[Result])(implicit request: Request[_],  hc: HeaderCarrier): Future[Result] = {
    keystoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString) flatMap {
      case Some(currentProfile) => payeRegistrationConnector.getStatus(currentProfile.registrationID) flatMap {
        case Some(status) => status match {
          case PAYEStatus.held | PAYEStatus.submitted => request.path match {
              // TODO - going to re-factor this under SCRS-6939
            case "/register-for-paye/confirmation" => f(currentProfile)
            case _               => Future.successful(Redirect(controllers.userJourney.routes.DashboardController.dashboard()))
          }
          case _                                      => f(currentProfile)
        }
        case None => throw new MissingDocumentStatus(s"There was no document status found for reg id ${currentProfile.registrationID}")
      }
      case None => Future.successful(Redirect(controllers.userJourney.routes.PayeStartController.startPaye()))
    }
  }
}
