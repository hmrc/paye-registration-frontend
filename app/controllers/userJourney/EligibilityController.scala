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

package controllers.userJourney

import javax.inject.Inject

import connectors.KeystoreConnector
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.eligibility.{CompanyEligibilityForm, DirectorEligibilityForm}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import services.{CompanyDetailsService, EligibilityService, IncorporationInformationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.inject.ServicesConfig
import views.html.pages.eligibility.{companyEligibility => CompanyEligibilityPage, directorEligibility => DirectorEligibilityPage, ineligible => IneligiblePage}

import scala.concurrent.Future

class EligibilityControllerImpl @Inject()(val messagesApi: MessagesApi,
                                          val keystoreConnector: KeystoreConnector,
                                          val eligibilityService: EligibilityService,
                                          val authConnector: AuthConnector,
                                          val env: Environment,
                                          val config: Configuration,
                                          val s4LService: S4LService,
                                          val companyDetailsService: CompanyDetailsService,
                                          val incorpInfoService: IncorporationInformationService,
                                          servicesConfig: ServicesConfig) extends EligibilityController with AuthRedirectUrls

trait EligibilityController extends PayeBaseController {
  val eligibilityService: EligibilityService

  val compRegFEURL: String
  val compRegFEURI: String

  def companyEligibility: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>profile =>
    eligibilityService.getEligibility(profile.registrationID) map {
      _.companyEligible match {
        case Some(boole) => Ok(CompanyEligibilityPage(CompanyEligibilityForm.form.fill(boole)))
        case _           => Ok(CompanyEligibilityPage(CompanyEligibilityForm.form))
      }
    }
  }

  def submitCompanyEligibility = isAuthorisedWithProfile { implicit request => profile =>
    CompanyEligibilityForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(CompanyEligibilityPage(errors))),
      success => eligibilityService.saveCompanyEligibility(profile.registrationID, success) map {
        _ => if(success.ineligible){
          Redirect(controllers.userJourney.routes.EligibilityController.ineligible())
        } else {
          Redirect(controllers.userJourney.routes.EligibilityController.directorEligibility())
        }
      }
    )
  }

  def directorEligibility: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    eligibilityService.getEligibility(profile.registrationID) map {
      _.directorEligible match {
        case Some(boole)  => Ok(DirectorEligibilityPage(DirectorEligibilityForm.form.fill(boole)))
        case _            => Ok(DirectorEligibilityPage(DirectorEligibilityForm.form))
      }
    }
  }

  def submitDirectorEligibility: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    DirectorEligibilityForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(DirectorEligibilityPage(errors))),
      success => eligibilityService.saveDirectorEligibility(profile.registrationID, success) map {
        _ => if(success.eligible){
          Redirect(controllers.userJourney.routes.EligibilityController.ineligible())
        } else {
          Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
        }
      }
    )
  }

  def ineligible: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(IneligiblePage()))
  }

  def questionnaire: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Redirect(s"$compRegFEURL$compRegFEURI/questionnaire"))
  }
}
