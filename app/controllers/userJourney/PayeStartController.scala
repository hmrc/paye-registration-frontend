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

package controllers.userJourney

import common.exceptions.{ConfirmationRefsNotFoundExceptionType, CurrentProfileNotFoundExceptionType}
import config.AppConfig
import connectors._
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.{CacheKeys, DownstreamOutcome, RegistrationDeletion}
import models.external.CurrentProfile
import play.api.i18n.Lang
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{Logging, PAYEFeatureSwitch}
import views.html.pages.error.restart

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PayeStartController @Inject()(val currentProfileService: CurrentProfileService,
                                    val payeRegistrationService: PAYERegistrationService,
                                    val keystoreConnector: KeystoreConnector,
                                    val authConnector: AuthConnector,
                                    val s4LService: S4LService,
                                    val companyDetailsService: CompanyDetailsService,
                                    val incorpInfoService: IncorporationInformationService,
                                    val businessRegistrationConnector: BusinessRegistrationConnector,
                                    val companyRegistrationConnector: CompanyRegistrationConnector,
                                    val featureSwitches: PAYEFeatureSwitch,
                                    val incorporationInformationConnector: IncorporationInformationConnector,
                                    mcc: MessagesControllerComponents,
                                    restart: restart,
                                    languageUtils: LanguageUtils
                                   )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc)
  with AuthRedirectUrls with Logging {

  private val welsh = Lang("cy")
  private val english = "english"

  def steppingStone(): Action[AnyContent] = Action { _ =>
    Redirect(s"$payeRegElFEURL$payeRegElFEURI")
  }

  def startPaye: Action[AnyContent] = isAuthorisedAndIsOrg { implicit request =>
    infoLog("[startPaye] attempting to startPaye")
    checkAndStoreCurrentProfile { profile =>
      assertPAYERegistrationFootprint(profile.registrationID, profile.companyTaxRegistration.transactionId) {
        if ((languageUtils.getCurrentLang == welsh) && !appConfig.languageTranslationEnabled) {
          Redirect(controllers.routes.LanguageSwitchController.setLanguage(english))
        } else {
          Redirect(routes.EmploymentController.paidEmployees)
        }
      }
    }
  }

  def restartPaye: Action[AnyContent] = isAuthorised { implicit request =>
    for {
      (regId, txId) <- getRegIdAndTxId
      deleted <- payeRegistrationService.deleteRejectedRegistration(regId, txId)
    } yield deleted match {
      case RegistrationDeletion.success => Redirect(routes.PayeStartController.startPaye)
      case RegistrationDeletion.invalidStatus => Redirect(controllers.userJourney.routes.DashboardController.dashboard)
    }
  }

  private def getRegIdAndTxId(implicit hc: HeaderCarrier, request: Request[_]): Future[(String, String)] = {
    keystoreConnector.fetchAndGet[CurrentProfile](CacheKeys.CurrentProfile.toString) flatMap {
      case Some(profile) => Future.successful((profile.registrationID, profile.companyTaxRegistration.transactionId))
      case None => for {
        businessProfile <- businessRegistrationConnector.retrieveCurrentProfile
        companyProfile <- companyRegistrationConnector.getCompanyRegistrationDetails(businessProfile.registrationID)
      } yield {
        (businessProfile.registrationID, companyProfile.transactionId)
      }
    }
  }

  private def checkAndStoreCurrentProfile(f: => CurrentProfile => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    infoLog(s"[checkAndStoreCurrentProfile] attempting to checkAndStoreCurrentProfile")
    currentProfileService.fetchAndStoreCurrentProfile flatMap { currentProfile: CurrentProfile =>
      currentProfileChecks(currentProfile)(f)
    } recover {
      case _: NotFoundException =>
        warnLog("[PayeStartController][checkAndStoreCurrentProfile] NotFoundException redirecting to OTRS")
        Redirect(appConfig.otrsUrl)
      case _: ConfirmationRefsNotFoundExceptionType =>
        warnLog("[PayeStartController][checkAndStoreCurrentProfile] ConfirmationRefsNotFoundException redirecting to OTRS")
        Redirect(appConfig.otrsUrl)
      case _: CurrentProfileNotFoundExceptionType =>
        warnLog("[PayeStartController][checkAndStoreCurrentProfile] no company profile found. Redirecting to OTRS")
        Redirect(appConfig.otrsUrl)
      case error =>
        errorLog(s"[checkAndStoreCurrentProfile] failed to checkAndStoreCurrentProfile. Error: $error")
        InternalServerError(restart())
    }
  }

  private def assertPAYERegistrationFootprint(regId: String, txId: String)(f: => Result)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    infoLog("[assertPAYERegistrationFootprint] attempting to assertPAYERegistrationFootprint")
    payeRegistrationService.assertRegistrationFootprint(regId, txId) map {
      case DownstreamOutcome.Success => f
      case DownstreamOutcome.Failure => InternalServerError(restart())
    }
  }
}
