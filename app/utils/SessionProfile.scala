/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import enums.{CacheKeys, IncorporationStatus, RegistrationDeletion}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Request, Result}
import services.PAYERegistrationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try

trait SessionProfile extends InternalExceptions {
  val keystoreConnector: KeystoreConnector
  val incorporationInformationConnector: IncorporationInformationConnector
  val payeRegistrationService: PAYERegistrationService

  def withCurrentProfile(f: => CurrentProfile => Future[Result], checkSubmissionStatus: Boolean = true)(implicit request: Request[_],  hc: HeaderCarrier): Future[Result] = {
    def ifInflightUserChecksElseRedirectTo(urlCall: Call): Future[Result] = {
      keystoreConnector.fetchAndGetFromKeystore(CacheKeys.CurrentProfile.toString) flatMap {
        _.fold(Future.successful(Redirect(urlCall))) { cp =>
          incorporationInformationConnector.setupSubscription(cp.companyTaxRegistration.transactionId, cp.registrationID) flatMap { res =>
            if (res.contains(IncorporationStatus.rejected)) {
              payeRegistrationService.handleIIResponse(cp.companyTaxRegistration.transactionId, res.get) map {
                case RegistrationDeletion.success => Redirect(controllers.userJourney.routes.SignInOutController.incorporationRejected())
                case _ =>
                  Logger.warn(s"Registration txId: ${cp.companyTaxRegistration.transactionId} - regId: ${cp.registrationID} incorporation is rejected but the cleanup failed probably due to the wrong status of the paye registration")
                  Redirect(controllers.userJourney.routes.SignInOutController.postSignIn())
              } recover {
                case err =>
                  Logger.error(s"Registration txId: ${cp.companyTaxRegistration.transactionId} - regId: ${cp.registrationID} Incorporation is rejected but handleIIResponse threw an unexpected exception whilst trying to cleanup with message: ${err.getMessage}")
                  Redirect(controllers.userJourney.routes.SignInOutController.incorporationRejected())
              }
            } else {
              currentProfileChecks(cp, checkSubmissionStatus)(f)
            }
          }
        }
      }
    }

    keystoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString) flatMap {
      case Some(currentProfile) => currentProfileChecks(currentProfile, checkSubmissionStatus)(f)
      case None                 => ifInflightUserChecksElseRedirectTo(controllers.userJourney.routes.PayeStartController.startPaye())
    }
  }

  protected[utils] def currentProfileChecks(currentProfile: CurrentProfile, checkSubmissionStatus: Boolean = true)(f: CurrentProfile => Future[Result]): Future[Result] = {
    currentProfile match {
      case CurrentProfile(_, CompanyRegistrationProfile(_, _, Some(a)), _, _, _) if Try(a.toInt).getOrElse(6) >= 6 =>
        Future.successful(Redirect(controllers.userJourney.routes.SignInOutController.postSignIn()))
      case CurrentProfile(_, compRegProfile, _, _, _) if compRegProfile.status equals "draft" =>
        Future.successful(Redirect("https://www.tax.service.gov.uk/business-registration/select-taxes"))
      case CurrentProfile(_, _, _, true, _) if checkSubmissionStatus =>
        Future.successful(Redirect(controllers.userJourney.routes.DashboardController.dashboard()))
      case CurrentProfile(_, _, _, _, Some(IncorporationStatus.rejected)) =>
        Future.successful(Redirect(controllers.userJourney.routes.SignInOutController.incorporationRejected()))
      case cp => f(cp)
    }
  }
}