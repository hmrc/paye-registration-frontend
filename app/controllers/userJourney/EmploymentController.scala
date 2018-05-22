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
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.employmentDetails._
import models.view.{EmployingStaff, Subcontractors}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.annual.FirstPaymentInNextTaxYear
import views.html.pages.employmentDetails.{companyPension => CompanyPensionPage, employingStaff => EmployingStaffPage, firstPayment => FirstPaymentPage, subcontractors => SubcontractorsPage}

import scala.concurrent.Future

class EmploymentControllerImpl @Inject()(val employmentService: EmploymentService,
                                         val keystoreConnector: KeystoreConnector,
                                         val config: Configuration,
                                         val authConnector: AuthConnector,
                                         val s4LService: S4LService,
                                         val companyDetailsService: CompanyDetailsService,
                                         val incorpInfoService: IncorporationInformationService,
                                         implicit val messagesApi: MessagesApi,
                                         val incorporationInformationConnector: IncorporationInformationConnector,
                                         val payeRegistrationService: PAYERegistrationService) extends EmploymentController with AuthRedirectUrls

trait EmploymentController extends PayeBaseController {
  val employmentService: EmploymentService

  // SUBCONTRACTORS
  def subcontractors: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      _.subcontractors match {
        case Some(model) => Ok(SubcontractorsPage(SubcontractorsForm.form.fill(model)))
        case _           => Ok(SubcontractorsPage(SubcontractorsForm.form))
      }
    }
  }

  def submitSubcontractors: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    SubcontractorsForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(SubcontractorsPage(errors))),
      model => employmentService.saveSubcontractors(model, profile.registrationID) map { model =>
        (model.employing, model.subcontractors) match {
          case (Some(EmployingStaff(false)), Some(Subcontractors(false))) => Redirect(controllers.errors.routes.ErrorController.ineligible())
          case _ => Redirect(controllers.userJourney.routes.EmploymentController.employingStaff())
        }
      }
    )
  }

  // EMPLOYING STAFF
  def employingStaff: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      _.employing match {
        case Some(model)  => Ok(EmployingStaffPage(EmployingStaffForm.form.fill(model)))
        case _            => Ok(EmployingStaffPage(EmployingStaffForm.form))
      }
    }
  }

  def submitEmployingStaff: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    EmployingStaffForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(EmployingStaffPage(errors))),
      model => employmentService.saveEmployingStaff(model, profile.registrationID) map { model =>
        (model.employing, model.subcontractors) match {
          case (Some(EmployingStaff(false)), Some(Subcontractors(false))) => Redirect(controllers.errors.routes.ErrorController.ineligible())
          case (Some(EmployingStaff(true)), _)                            => Redirect(controllers.userJourney.routes.EmploymentController.companyPension())
          case _                                                          => Redirect(controllers.userJourney.routes.EmploymentController.firstPayment())
        }
      }
    )
  }

  // COMPANY PENSION
  def companyPension: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      _.companyPension match {
        case Some(model)  => Ok(CompanyPensionPage(CompanyPensionForm.form.fill(model)))
        case _            => Ok(CompanyPensionPage(CompanyPensionForm.form))
      }
    }
  }

  def submitCompanyPension: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    CompanyPensionForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(CompanyPensionPage(errors))),
      model => employmentService.saveCompanyPension(model, profile.registrationID) map {
        _ => Redirect(controllers.userJourney.routes.EmploymentController.firstPayment())
      }
    )
  }

  // FIRST PAYMENT
  def firstPayment: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      _.firstPayment match {
        case Some(model) => Ok(FirstPaymentPage(FirstPaymentForm.form.fill(model)))
        case _           => Ok(FirstPaymentPage(FirstPaymentForm.form))
      }
    }
  }

  def submitFirstPayment = isAuthorisedWithProfile { implicit request => profile =>
    FirstPaymentForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(FirstPaymentPage(errors))),
      model => for {
        _                           <- employmentService.saveFirstPayment(model, profile.registrationID)
        firstPaymentDateInNextYear  =  employmentService.firstPaymentDateInNextYear(model.firstPayDate)
      } yield if(firstPaymentDateInNextYear) {
        Redirect(controllers.userJourney.routes.EmploymentController.ifFirstPaymentIsInTheNextTaxYear() )
      } else {
        Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
      }
    )
  }

  def ifFirstPaymentIsInTheNextTaxYear: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Ok(FirstPaymentInNextTaxYear()))
  }

  def redirectBackToStandardFlow: Action[AnyContent] = isAuthorisedWithProfile { implicit request => _ =>
    Future.successful(Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity()))
  }
}
