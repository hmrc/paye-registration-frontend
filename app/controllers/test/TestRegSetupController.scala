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

package controllers.test

import javax.inject.Inject
import connectors._
import connectors.test.{TestBusinessRegConnector, TestPAYERegConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import forms.test.{TestPAYEContactForm, TestPAYERegCompanyDetailsSetupForm, TestPAYERegEmploymentInfoSetupForm, TestPAYERegSetupForm}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class TestRegSetupControllerImpl @Inject()(val payeRegService:PAYERegistrationService,
                                           val testPAYERegConnector: TestPAYERegConnector,
                                           val keystoreConnector: KeystoreConnector,
                                           val testBusinessRegConnector: TestBusinessRegConnector,
                                           val authConnector: AuthConnector,
                                           val config: Configuration,
                                           val s4LService: S4LService,
                                           val companyDetailsService: CompanyDetailsService,
                                           val incorpInfoService: IncorporationInformationService,
                                           val messagesApi: MessagesApi) extends TestRegSetupController with AuthRedirectUrls

trait TestRegSetupController extends PayeBaseController {
  val payeRegService: PAYERegistrationService
  val testPAYERegConnector: TestPAYERegConnector
  val testBusinessRegConnector: TestBusinessRegConnector

  def regTeardown: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    testPAYERegConnector.testRegistrationTeardown() map {
      case DownstreamOutcome.Success => Ok("Registration collection successfully cleared")
      case DownstreamOutcome.Failure => InternalServerError("Error clearing registration collection")
    }
  }

  protected[controllers] def doIndividualRegTeardown(regId: String)(implicit request: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    testPAYERegConnector.tearDownIndividualRegistration(regId)
  }

  def individualRegTeardown(regId: String): Action[AnyContent] = isAuthorised { implicit request =>
    doIndividualRegTeardown(regId) map {
      case DownstreamOutcome.Success => Ok(s"Registration successfully cleared for regID $regId")
      case DownstreamOutcome.Failure => InternalServerError(s"Error clearing registration for regID $regId")
    }
  }

  def regSetup: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    Future.successful(Ok(views.html.pages.test.payeRegistrationSetup(TestPAYERegSetupForm.form, profile.registrationID, profile.companyTaxRegistration.transactionId)))
  }

  def submitRegSetup: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    TestPAYERegSetupForm.form.bindFromRequest.fold(
      errors =>
        Future.successful(BadRequest(views.html.pages.test.payeRegistrationSetup(errors, profile.registrationID, profile.companyTaxRegistration.transactionId))),
      success =>
        for {
          setupReg <- testPAYERegConnector.addPAYERegistration(success)
          _        <- testBusinessRegConnector.updateCompletionCapacity(profile.registrationID, success.completionCapacity)
        } yield {
          setupReg match {
            case DownstreamOutcome.Success => Ok("PAYE Registration set up successfully")
            case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Registration")
          }
        }
    )
  }

  def regSetupCompanyDetails: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegCompanyDetailsSetup(TestPAYERegCompanyDetailsSetupForm.form)))
  }

  def submitRegSetupCompanyDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    TestPAYERegCompanyDetailsSetupForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(views.html.pages.test.payeRegCompanyDetailsSetup(errors))),
      success => testPAYERegConnector.addTestCompanyDetails(success, profile.registrationID) map {
        case DownstreamOutcome.Success => Ok("Company details successfully set up")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up Company Details")
      }
    )
  }

  def regSetupPAYEContact: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegPAYEContactSetup(TestPAYEContactForm.form)))
  }

  def submitRegSetupPAYEContact: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    TestPAYEContactForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(views.html.pages.test.payeRegPAYEContactSetup(errors))),
      success => testPAYERegConnector.addTestPAYEContact(success, profile.registrationID) map {
        case DownstreamOutcome.Success => Ok("PAYE Contact details successfully set up")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Contact details")
      }
    )
  }

  def regSetupEmploymentInfo: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegEmploymentInfoSetup(TestPAYERegEmploymentInfoSetupForm.form)))
  }

  def submitRegSetupEmploymentInfo: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    TestPAYERegEmploymentInfoSetupForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(views.html.pages.test.payeRegEmploymentInfoSetup(errors))),
      success => testPAYERegConnector.addTestEmploymentInfo(success, profile.registrationID) map {
        case DownstreamOutcome.Success => Ok("Employment info successfully set up")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up Employment info")
      }
    )
  }
}
