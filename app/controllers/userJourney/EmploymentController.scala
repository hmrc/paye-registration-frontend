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

import connectors.{S4LConnector, PAYERegistrationConnector, KeystoreConnector}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import config.{PAYEShortLivedCache, PAYESessionCache, FrontendAuthConnector}
import auth.PAYERegime
import enums.DownstreamOutcome
import forms.employmentDetails._

import scala.concurrent.Future
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{S4LService, EmploymentService}
import uk.gov.hmrc.play.frontend.auth.Actions
import views.html.pages.employmentDetails.{employingStaff => EmployingStaffPage}
import views.html.pages.employmentDetails.{companyPension => CompanyPensionPage}
import views.html.pages.employmentDetails.{subcontractors => SubcontractorsPage}
import views.html.pages.employmentDetails.{firstPayment => FirstPaymentPage}

object EmploymentController extends EmploymentController {
  //$COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val employmentService = new EmploymentService(new KeystoreConnector(new PAYESessionCache()), new PAYERegistrationConnector(), new S4LService(new S4LConnector(new PAYEShortLivedCache()), new KeystoreConnector(new PAYESessionCache())))
  //$COVERAGE-ON$
}

trait EmploymentController extends FrontendController with Actions {
  val employmentService: EmploymentService

  // EMPLOYING STAFF
  val employingStaff = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      employment   <- employmentService.fetchEmploymentView()
    } yield employment.employing match {
      case Some(model) => Ok(EmployingStaffPage(EmployingStaffForm.form.fill(model)))
      case _ => Ok(EmployingStaffPage(EmployingStaffForm.form))
    }
  }

  val submitEmployingStaff = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    EmployingStaffForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(EmployingStaffPage(errors))),
      model => employmentService.saveEmployingStaff(model) map {
        case DownstreamOutcome.Success => model.currentYear match {
          case true => Redirect(controllers.userJourney.routes.EmploymentController.companyPension())
          case false => Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
        }
      }
    )
  }

  // COMPANY PENSION
  val companyPension = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      employment   <- employmentService.fetchEmploymentView()
    } yield employment.companyPension match {
      case Some(model) => Ok(CompanyPensionPage(CompanyPensionForm.form.fill(model)))
      case _ => Ok(CompanyPensionPage(CompanyPensionForm.form))
    }
  }

  val submitCompanyPension = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    CompanyPensionForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(CompanyPensionPage(errors))),
      model => employmentService.saveCompanyPension(model) map {
        case DownstreamOutcome.Success => model.pensionProvided match {
          case true => Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
          case false => Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
        }
      }
    )
  }

  // SUBCONTRACTORS
  val subcontractors = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      employment   <- employmentService.fetchEmploymentView()
    } yield employment.subcontractors match {
      case Some(model) => Ok(SubcontractorsPage(SubcontractorsForm.form.fill(model)))
      case _ => Ok(SubcontractorsPage(SubcontractorsForm.form))
    }
  }

  val submitSubcontractors = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    SubcontractorsForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(SubcontractorsPage(errors))),
      model => employmentService.saveSubcontractors(model) map {
        case DownstreamOutcome.Success => model.hasContractors match {
          case true => Redirect(controllers.userJourney.routes.EmploymentController.firstPayment())
          case false => Redirect(controllers.userJourney.routes.EmploymentController.firstPayment())
        }
      }
    )
  }

  // FIRST PAYMENT
  val firstPayment = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      employment   <- employmentService.fetchEmploymentView()
    } yield employment.firstPayment match {
      case Some(model) => Ok(FirstPaymentPage(FirstPaymentForm.form.fill(model)))
      case _ => Ok(FirstPaymentPage(FirstPaymentForm.form))
    }
  }

  val submitFirstPayment = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    FirstPaymentForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(FirstPaymentPage(errors))),
      model => employmentService.saveFirstPayment(model) map (_ => Redirect(controllers.userJourney.routes.SummaryController.summary()))
    )
  }
}
