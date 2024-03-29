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

package controllers

import utils.Logging
import config.AppConfig
import controllers.userJourney.{routes => userJourneyRoutes}
import models.external.{AuditingInformation, CurrentProfile}
import play.api.i18n.I18nSupport
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionProfile

import scala.concurrent.Future

abstract class PayeBaseController(mcc: MessagesControllerComponents) extends FrontendController(mcc) with AuthorisedFunctions with Logging with SessionProfile with I18nSupport {

  type AuthorisedActionWithProfile = Request[AnyContent] => CurrentProfile => Future[Result]
  type AuthorisedActionWithProfileAndAuditingInfo = Request[AnyContent] => CurrentProfile => AuditingInformation => Future[Result]

  def redirectToLogin: Result

  def redirectToPostSign: Result

  def isAuthorised(f: => (Request[AnyContent] => Future[Result])): Action[AnyContent] = Action.async { implicit request =>
    authorised(ConfidenceLevel.L50) {
      f(request)
    } recover {
      case e: InsufficientConfidenceLevel =>
        warnLog(s"[isAuthorised] unauthenticated user attempting to access ${request.path} with insufficient confidence level redirecting to login")
        redirectToLogin
      case e: AuthorisationException =>
        infoLog(s"[isAuthorised] unauthenticated user attempting to access ${request.path} redirecting to login : ${e.getMessage}")
        redirectToLogin
    }
  }

  def isAuthorisedWithProfile(f: => AuthorisedActionWithProfile): Action[AnyContent] = Action.async { implicit request =>
    authorised(ConfidenceLevel.L50) {
      withCurrentProfile(profile => f(request)(profile))
    } recover {
      case _: InsufficientConfidenceLevel =>
        warnLog(s"[isAuthorisedWithProfile] unauthenticated user attempting to access ${request.path} with insufficient confidence level redirecting to login")
        redirectToLogin
      case e: AuthorisationException =>
        infoLog(s"[isAuthorisedWithProfile] unauthenticated user attempting to access ${request.path} redirecting to login: ${e.getMessage}")
        redirectToLogin
    }
  }

  def isAuthorisedWithProfileNoSubmissionCheck(f: => AuthorisedActionWithProfile): Action[AnyContent] = Action.async { implicit request =>
    authorised(ConfidenceLevel.L50) {
      withCurrentProfile(profile =>
        f(request)(profile),
        checkSubmissionStatus = false
      )
    } recover {
      case _: InsufficientConfidenceLevel =>
        warnLog(s"[isAuthorisedWithProfileNoSubmissionCheck] unauthenticated user attempting to access ${request.path} with insufficient confidence level redirecting to loginUrl")
        redirectToLogin
      case e: AuthorisationException =>
        infoLog(s"[isAuthorisedWithProfileNoSubmissionCheck] unauthenticated user attempting to access ${request.path} redirecting to login: ${e.getMessage}")
        redirectToLogin
    }
  }

  def isAuthorisedAndIsOrg(f: => (Request[AnyContent] => Future[Result])): Action[AnyContent] = Action.async { implicit request =>
    authorised(ConfidenceLevel.L50).retrieve(affinityGroup) { aG =>
      if (aG.contains(Organisation)) {
        f(request)
      } else {
        warnLog(s"[isAuthorisedAndIsOrg] User attempting to access ${request.path} doesn't have org affinity redirecting to OTRS")
        Future(Redirect("https://www.tax.service.gov.uk/business-registration/select-taxes"))
      }
    } recover {
      case _: InsufficientConfidenceLevel =>
        warnLog(s"[isAuthorisedAndIsOrg] unauthenticated user attempting to access ${request.path} with insufficient confidence level redirecting to login")
        redirectToLogin
      case e: UnsupportedAffinityGroup =>
        warnLog(s"[isAuthorisedAndIsOrg] unauthenticated user attempting to access ${request.path} with incorrect affinity group redirecting to login")
        redirectToLogin
      case e: AuthorisationException =>
        infoLog(s"[isAuthorisedAndIsOrg] unauthenticated user attempting to access ${request.path} redirecting to login : ${e.getMessage}")
        redirectToLogin
    }
  }

  def isAuthorisedWithProfileAndAuditing(f: => AuthorisedActionWithProfileAndAuditingInfo): Action[AnyContent] = Action.async { implicit request =>
    authorised(ConfidenceLevel.L50).retrieve(externalId and credentials) {
      case Some(exId) ~ Some(creds) =>
        val auditingInformation = AuditingInformation(exId, creds.providerId)
        withCurrentProfile(profile => f(request)(profile)(auditingInformation))
      case _ =>
        warnLog(s"[isAuthorisedWithProfileAndAuditing] User attempting to access ${request.path} doesn't have either externalId or credentials redirecting to $redirectToPostSign")
        Future.successful(redirectToPostSign)
    } recover {
      case _: InsufficientConfidenceLevel =>
        warnLog(s"[isAuthorisedWithProfileAndAuditing] unauthenticated user attempting to access ${request.path} with insufficient confidence level redirecting to login")
        redirectToLogin
      case e: AuthorisationException =>
        infoLog(s"[isAuthorisedWithProfileAndAuditing] unauthenticated user attempting to access ${request.path} redirecting to login: ${e.getMessage}")
        redirectToLogin
    }
  }

}

trait AuthRedirectUrls {
  val appConfig: AppConfig

  private val configRoot = "microservice.services"

  private lazy val appName = appConfig.servicesConfig.getString("appName")

  private lazy val loginCallback = appConfig.servicesConfig.getString(s"$configRoot.auth.login-callback.url")

  private lazy val buildCompanyAuthUrl = {
    val basGatewayHost = appConfig.servicesConfig.getString(s"$configRoot.auth.bas-gateway.url")
    val loginPath = appConfig.servicesConfig.getString(s"$configRoot.auth.login_path")
    s"$basGatewayHost$loginPath"
  }

  private lazy val continueUrl = s"$loginCallback${userJourneyRoutes.PayeStartController.startPaye}"

  lazy val redirectToLogin: Result = Redirect(buildCompanyAuthUrl, Map(
    "continue_url" -> Seq(continueUrl),
    "origin" -> Seq(appName)
  ))

  lazy val redirectToPostSign = Redirect(userJourneyRoutes.SignInOutController.postSignIn)

  lazy val compRegFEURL = appConfig.servicesConfig.getString(s"$configRoot.company-registration-frontend.www.url")
  lazy val compRegFEURI = appConfig.servicesConfig.getString(s"$configRoot.company-registration-frontend.www.uri")

  lazy val payeRegElFEURL = appConfig.servicesConfig.getString(s"$configRoot.paye-registration-eligibility-frontend.www.url")
  lazy val payeRegElFEURI = appConfig.servicesConfig.getString(s"$configRoot.paye-registration-eligibility-frontend.www.uri")

}