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

package controllers.userJourney

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnector}
import views.html.pages.eligibility.{companyEligibility => CompanyEligibilityPage, directorEligibility => DirectorEligibilityPage, ineligible => IneligiblePage}
import forms.eligibility.{CompanyEligibilityForm, DirectorEligibilityForm}
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{EligibilityService, EligibilitySrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class EligibilityController @Inject()(injMessagesApi: MessagesApi,
                                      injKeystoreConnector: KeystoreConnector,
                                      injEligibilityService: EligibilityService,
                                      injPayeRegistrationConnector: PAYERegistrationConnector
                                     ) extends EligibilityCtrl with ServicesConfig {
  val authConnector = FrontendAuthConnector
  val messagesApi = injMessagesApi
  override val eligibilityService = injEligibilityService
  override val keystoreConnector = injKeystoreConnector
  val payeRegistrationConnector = injPayeRegistrationConnector
  lazy val compRegFEURL = getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = getConfString("company-registration-frontend.www.uri", "")
}

trait EligibilityCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val keystoreConnector: KeystoreConnect
  val eligibilityService: EligibilitySrv
  val compRegFEURL: String
  val compRegFEURI: String

  val companyEligibility = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { profile =>
        for {
          storedData <- eligibilityService.getEligibility(profile.registrationID)
        } yield storedData.companyEligible match {
          case Some(boole) => Ok(CompanyEligibilityPage(CompanyEligibilityForm.form.fill(boole)))
          case _ => Ok(CompanyEligibilityPage(CompanyEligibilityForm.form))
        }
      }
  }

  val submitCompanyEligibility = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { profile =>
        CompanyEligibilityForm.form.bindFromRequest.fold(
          errors => Future.successful(BadRequest(CompanyEligibilityPage(errors))),
          success =>
            eligibilityService.saveCompanyEligibility(profile.registrationID, success) map {
              _ => if(success.ineligible){
                Redirect(controllers.userJourney.routes.EligibilityController.ineligible())
              } else {
                Redirect(controllers.userJourney.routes.EligibilityController.directorEligibility())
              }
            }
        )
      }
  }

  val directorEligibility = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { profile =>
        for {
          storedData <- eligibilityService.getEligibility(profile.registrationID)
        } yield storedData.directorEligible match {
          case Some(boole) => Ok(DirectorEligibilityPage(DirectorEligibilityForm.form.fill(boole)))
          case _ => Ok(DirectorEligibilityPage(DirectorEligibilityForm.form))
        }
      }
  }

  val submitDirectorEligibility = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { profile =>
        DirectorEligibilityForm.form.bindFromRequest.fold(
          errors => Future.successful(BadRequest(DirectorEligibilityPage(errors))),
          success =>
            eligibilityService.saveDirectorEligibility(profile.registrationID, success) map {
              _ => if(success.eligible){
                Redirect(controllers.userJourney.routes.EligibilityController.ineligible())
              } else {
                Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
              }
            }
        )
      }
  }

  val ineligible = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { _ =>
        Future.successful(Ok(IneligiblePage()))
      }
  }

  val questionnaire = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Redirect(s"$compRegFEURL$compRegFEURI/questionnaire")
  }
}
