/*
 * Copyright 2022 HM Revenue & Customs
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

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.employmentDetails._
import models.view._
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.{SystemDate, SystemDateT}
import views.html.pages.employmentDetails._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentController @Inject()(employmentService: EmploymentService,
                                     thresholdService: ThresholdService,
                                     val keystoreConnector: KeystoreConnector,
                                     val authConnector: AuthConnector,
                                     incorpInfoService: IncorporationInformationService,
                                     val incorporationInformationConnector: IncorporationInformationConnector,
                                     val payeRegistrationService: PAYERegistrationService,
                                     mcc: MessagesControllerComponents,
                                     PaidEmployeesPage: paidEmployees,
                                     willBePayingPage: willBePaying,
                                     ConstructionIndustryPage: constructionIndustry,
                                     ApplicationDelayedPage: applicationDelayed,
                                     SubcontractorsPage: employsSubcontractors,
                                     PaysPensionPage: paysPension
                                    )(implicit val appConfig: AppConfig,
                                      val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  val taxYearObjWithSystemDate: SystemDateT = SystemDate

  private val handleJourneyPostConstruction: EmployingStaff => Result = {
    case EmployingStaff(Some(EmployingAnyone(true, _)), _, _, _, _) => Redirect(controllers.userJourney.routes.EmploymentController.pensions)
    case EmployingStaff(_, Some(WillBePaying(true, _)), _, _, _) | EmployingStaff(_, Some(WillBePaying(false, _)), Some(true), _, _) =>
      Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity)
    case EmployingStaff(_, Some(WillBePaying(false, _)), Some(false), _, _) => Redirect(controllers.errors.routes.ErrorController.newIneligible)
    case _ => throw new InternalServerException(s"[EmploymentController][handleJourneyPostConstruction] an invalid scenario was met for employment staff")
  }

  def weeklyThreshold: Int = thresholdService.getCurrentThresholds("weekly")

  private def ifIncorpDateExist(regId: String, txId: String)(action: LocalDate => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    incorpInfoService.getIncorporationDate(regId, txId) flatMap {
      _.fold(Future.successful(Redirect(controllers.userJourney.routes.EmploymentController.employingStaff)))(action)
    }

  // PAID EMPLOYEES
  def paidEmployees: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      ifIncorpDateExist(profile.registrationID, profile.companyTaxRegistration.transactionId) { incorpDate =>
        employmentService.fetchEmployingStaff map { viewModel =>
          val form = viewModel.employingAnyone.fold(PaidEmployeesForm.form(incorpDate))(PaidEmployeesForm.form(incorpDate).fill)
          Ok(PaidEmployeesPage(form, weeklyThreshold))
        }
      }
  }

  def submitPaidEmployees: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      ifIncorpDateExist(profile.registrationID, profile.companyTaxRegistration.transactionId) { incorpDate =>
        PaidEmployeesForm.form(incorpDate).bindFromRequest.fold(
          errors => Future.successful(BadRequest(PaidEmployeesPage(errors, weeklyThreshold))),
          model => {
            employmentService.saveEmployingAnyone(model) map { model =>
              model.employingAnyone match {
                case Some(EmployingAnyone(false, _)) =>
                  Redirect(controllers.userJourney.routes.EmploymentController.employingStaff)
                case _ =>
                  Redirect(controllers.userJourney.routes.EmploymentController.constructionIndustry)
              }
            }
          }
        )
      }
  }

  def employingStaff: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      employmentService.fetchEmployingStaff map { viewModel =>
        val now = taxYearObjWithSystemDate.getSystemDate.toLocalDate
        val form = viewModel.willBePaying.fold(EmployingStaffForm.form(now))(EmployingStaffForm.form(now).fill)
        Ok(willBePayingPage(form, weeklyThreshold, now))
      }
  }

  def submitEmployingStaff: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      val now = taxYearObjWithSystemDate.getSystemDate.toLocalDate
      EmployingStaffForm.form(now).bindFromRequest().fold(
        errors => Future.successful(BadRequest(willBePayingPage(errors, weeklyThreshold, now))),
        willBePaying => employmentService.saveWillEmployAnyone(willBePaying).map {
          _.willBePaying match {
            case Some(WillBePaying(true, Some(false))) => Redirect(controllers.userJourney.routes.EmploymentController.applicationDelayed)
            case None =>
              throw new InternalServerException(s"[EmploymentController][SubmitEmployingStaff] no WillBePaying block found on save for regId: ${profile.registrationID}")
            case _ => Redirect(controllers.userJourney.routes.EmploymentController.constructionIndustry)
          }
        }
      )
  }

  // CONSTRUCTION INDUSTRY
  def constructionIndustry: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      employmentService.fetchEmployingStaff map {
        viewModel =>
          val form = viewModel.construction.fold(ConstructionIndustryForm.form)(ConstructionIndustryForm.form.fill)
          Ok(ConstructionIndustryPage(form))
      }
  }

  def submitConstructionIndustry: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      ConstructionIndustryForm.form.bindFromRequest().fold(
        errors => Future.successful(BadRequest(ConstructionIndustryPage(errors))),
        cis => employmentService.saveConstructionIndustry(cis) map {
          viewModel =>
            if (cis) {
              Redirect(controllers.userJourney.routes.EmploymentController.subcontractors)
            } else {
              handleJourneyPostConstruction(viewModel)
            }
        }
      )
  }

  // APPLICATION DELAYED
  def applicationDelayed: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(ApplicationDelayedPage()))
  }

  def submitApplicationDelayed: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Redirect(controllers.userJourney.routes.EmploymentController.constructionIndustry))
  }

  // SUBCONTRACTORS
  def subcontractors: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>

      employmentService.fetchEmployingStaff map {
        viewModel =>
          val form = viewModel.subcontractors.fold(SubcontractorsForm.form)(SubcontractorsForm.form.fill)
          Ok(SubcontractorsPage(form, SystemDate.current.startYear.toString, SystemDate.current.finishYear.toString))
      }
  }

  def submitSubcontractors: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      SubcontractorsForm.form.bindFromRequest().fold(
        errors => Future.successful(BadRequest(SubcontractorsPage(errors, SystemDate.current.startYear.toString, SystemDate.current.finishYear.toString))),
        employsSubcontractors => employmentService.saveSubcontractors(employsSubcontractors).map(handleJourneyPostConstruction)
      )
  }

  // PENSIONS
  def pensions: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      employmentService.fetchEmployingStaff map {
        viewModel =>
          val form = viewModel.companyPension.fold(PaysPensionForm.form)(PaysPensionForm.form.fill)
          Ok(PaysPensionPage(form))
      }
  }

  def submitPensions: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    implicit profile =>
      PaysPensionForm.form.bindFromRequest().fold(
        errors => Future.successful(BadRequest(PaysPensionPage(errors))),
        paysPension => employmentService.savePensionPayment(paysPension) map {
          _ => Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity)
        }
      )
  }
}