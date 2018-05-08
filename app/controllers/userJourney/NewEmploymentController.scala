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

import java.time.LocalDate

import javax.inject.Inject
import connectors.KeystoreConnector
import controllers.exceptions.{FrontendControllerException, GeneralException}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.employmentDetails._
import models.view._
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.employmentDetails.{applicationDelayed => ApplicationDelayedPage, constructionIndustry => ConstructionIndustryPage, employsSubcontractors => SubcontractorsPage, paidEmployees => PaidEmployeesPage, paysPension => PaysPensionPage, willBePaying => EmployingStaffPage}

import scala.concurrent.Future

class NewEmploymentControllerImpl @Inject()(val employmentService: EmploymentServiceV2,
                                            val thresholdService: ThresholdService,
                                            val keystoreConnector: KeystoreConnector,
                                            val config: Configuration,
                                            val authConnector: AuthConnector,
                                            val s4LService: S4LService,
                                            val companyDetailsService: CompanyDetailsService,
                                            val incorpInfoService: IncorporationInformationService,
                                            implicit val messagesApi: MessagesApi) extends NewEmploymentController with AuthRedirectUrls


trait NewEmploymentController extends PayeBaseController {
  val employmentService: EmploymentServiceV2
  val thresholdService: ThresholdService
  val incorpInfoService: IncorporationInformationService

  private val handleJourneyPostConstruction: EmployingStaffV2 => Result = {
    case EmployingStaffV2(Some(EmployingAnyone(true, _)), _, _, _, None) => Redirect(controllers.userJourney.routes.NewEmploymentController.pensions())
    case EmployingStaffV2(_, Some(WillBePaying(true, _)), _, _, None) | EmployingStaffV2(_, Some(WillBePaying(false, _)), Some(true), _, None)  =>
      Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
    case EmployingStaffV2(_, Some(WillBePaying(false, _)), Some(false), _, None) => Redirect(controllers.errors.routes.ErrorController.newIneligible())
    case data => throw GeneralException(s"[NewEmploymentController][handleJourneyPostConstruction] an invalid scenario was met for employment staff v2: $data")
  }

  def weeklyThreshold: Int = thresholdService.getCurrentThresholds.getOrElse("weekly", 116)

  private def ifIncorpDateExist(regId: String, txId: String)(action: LocalDate => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    incorpInfoService.getIncorporationDate(regId, txId) flatMap {
      _.fold(Future.successful(Redirect(controllers.userJourney.routes.NewEmploymentController.employingStaff())))(action)
    } recover {
      case e: FrontendControllerException => e.recover
    }

  // PAID EMPLOYEES
  def paidEmployees: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    ifIncorpDateExist(profile.registrationID, profile.companyTaxRegistration.transactionId) { incorpDate =>
      employmentService.fetchEmploymentView(profile.registrationID) map { viewModel =>
        val form = viewModel.employingAnyone.fold(PaidEmployeesForm.form(incorpDate))(PaidEmployeesForm.form(incorpDate).fill)
        Ok(PaidEmployeesPage(form, weeklyThreshold))
      }
    } recover {
      case e : FrontendControllerException => e.recover
    }
  }

  def submitPaidEmployees: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      ifIncorpDateExist(profile.registrationID, profile.companyTaxRegistration.transactionId) { incorpDate =>
        PaidEmployeesForm.form(incorpDate).bindFromRequest.fold(
          errors => Future.successful(BadRequest(PaidEmployeesPage(errors, weeklyThreshold))),
          model => {
            employmentService.saveEmployingAnyone(profile.registrationID, model) map { model =>
              model.employingAnyone match {
                case Some(EmployingAnyone(false, _)) => Redirect(controllers.userJourney.routes.NewEmploymentController.employingStaff())
                case Some(EmployingAnyone(true, _)) => Redirect(controllers.userJourney.routes.NewEmploymentController.constructionIndustry())
              }
            }
          }
        )
      } recover {
        case e : FrontendControllerException => e.recover
      }
    }

  def employingStaff: Action[AnyContent] = isAuthorisedWithProfile{ implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map { viewModel =>
      val form = viewModel.willBePaying.fold(EmployingStaffFormV2.form)(EmployingStaffFormV2.form.fill)
      Ok(EmployingStaffPage(form, weeklyThreshold))
    } recover {
      case e : FrontendControllerException => e.recover
    }
  }

  def submitEmployingStaff: Action[AnyContent] = isAuthorisedWithProfile{ implicit request => profile =>
    EmployingStaffFormV2.form.bindFromRequest().fold({
      errors => Future.successful(BadRequest(EmployingStaffPage(errors, weeklyThreshold)))
    },{
      willBePaying => employmentService.saveWillEmployAnyone(profile.registrationID, willBePaying).map {
        _.willBePaying match {
          case Some(WillBePaying(true, Some(false)))  => Redirect(controllers.userJourney.routes.NewEmploymentController.applicationDelayed())
          case None                                   => NotImplemented //TODO should we error if nothing there
          case _                                      => Redirect(controllers.userJourney.routes.NewEmploymentController.constructionIndustry())
        }
      }
    }) recover {
      case e : FrontendControllerException => e.recover
    }
  }

  // CONSTRUCTION INDUSTRY
  def constructionIndustry: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      viewModel => val form = viewModel.companyPension.fold(ConstructionIndustryForm.form)(ConstructionIndustryForm.form.fill)
        Ok(ConstructionIndustryPage(form))
    } recover {
      case e : FrontendControllerException => e.recover
    }
  }

  def submitConstructionIndustry: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    ConstructionIndustryForm.form.bindFromRequest().fold({
      errors => Future.successful(BadRequest(ConstructionIndustryPage(errors)))
    },{
      cis => employmentService.saveConstructionIndustry(profile.registrationID, cis) map {
        viewModel => if(cis){
          Redirect(controllers.userJourney.routes.NewEmploymentController.subcontractors())
        } else {
          handleJourneyPostConstruction(viewModel)
        }
      }
    }) recover {
      case e : FrontendControllerException => e.recover
    }
  }

  // APPLICATION DELAYED
  def applicationDelayed: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(ApplicationDelayedPage()))
  }

  def submitApplicationDelayed: Action[AnyContent] = isAuthorised { implicit request =>
     Future.successful(Redirect(controllers.userJourney.routes.NewEmploymentController.constructionIndustry()))
  }

  // SUBCONTRACTORS
  def subcontractors: Action[AnyContent] = isAuthorisedWithProfile{ implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      viewModel => val form = viewModel.subcontractors.fold(SubcontractorsFormV2.form)(SubcontractorsFormV2.form.fill)
        Ok(SubcontractorsPage(form))
    } recover {
      case e : FrontendControllerException => e.recover
    }
  }

  def submitSubcontractors: Action[AnyContent] =  isAuthorisedWithProfile{ implicit request => profile =>
    SubcontractorsFormV2.form.bindFromRequest().fold({
      errors => Future.successful(BadRequest(SubcontractorsPage(errors)))
    },{
      employsSubcontractors => employmentService.saveSubcontractors(profile.registrationID, employsSubcontractors).map(handleJourneyPostConstruction)
    }).recover {
      case e : FrontendControllerException => e.recover
      case e: Exception => GeneralException(s"ouch ${e.getMessage}").recover
    }
  }

  // PENSIONS
  def pensions: Action[AnyContent] = isAuthorisedWithProfile{ implicit request => profile =>
    employmentService.fetchEmploymentView(profile.registrationID) map {
      viewModel => val form = viewModel.companyPension.fold(PaysPensionForm.form)(PaysPensionForm.form.fill)
        Ok(PaysPensionPage(form))
    } recover {
      case e : FrontendControllerException => e.recover
    }
  }

  def submitPensions: Action[AnyContent] =  isAuthorisedWithProfile{ implicit request => profile =>
    PaysPensionForm.form.bindFromRequest().fold({
      errors => Future.successful(BadRequest(PaysPensionPage(errors)))
    },{
      paysPension => employmentService.savePensionPayment(profile.registrationID, paysPension) map {
        _ => Redirect(controllers.userJourney.routes.CompletionCapacityController.completionCapacity())
      }
    }) recover {
      case e : FrontendControllerException => e.recover
    }
  }

}
