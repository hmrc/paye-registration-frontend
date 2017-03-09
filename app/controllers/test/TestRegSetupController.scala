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

package controllers.test

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector}
import connectors.test.{TestPAYERegConnect, TestPAYERegConnector}
import enums.DownstreamOutcome
import forms.test.{TestPAYEContactForm, TestPAYERegCompanyDetailsSetupForm, TestPAYERegSetupForm}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import services.{PAYERegistrationService, PAYERegistrationSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class TestRegSetupController @Inject()(injPayeRegService:PAYERegistrationService,
                                       injTestPAYERegConnector: TestPAYERegConnector,
                                       injKeystoreConnector: KeystoreConnector,
                                       injMessagesApi: MessagesApi)
  extends TestRegSetupCtrl {
  val authConnector = FrontendAuthConnector
  val payeRegService = injPayeRegService
  val testPAYERegConnector = injTestPAYERegConnector
  val messagesApi = injMessagesApi
  val keystoreConnector = injKeystoreConnector
}

trait TestRegSetupCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val payeRegService: PAYERegistrationSrv
  val testPAYERegConnector: TestPAYERegConnect
  val keystoreConnector: KeystoreConnect

  protected[controllers] def doRegTeardown(implicit request: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    testPAYERegConnector.testRegistrationTeardown()
  }

  val regTeardown = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            res <- doRegTeardown
          } yield res match {
            case DownstreamOutcome.Success => Ok("Registration collection successfully cleared")
            case DownstreamOutcome.Failure => InternalServerError("Error clearing registration collection")
          }
        }
  }

  val regSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          Future.successful(Ok(views.html.pages.test.payeRegistrationSetup(TestPAYERegSetupForm.form, profile.registrationID)))
        }
  }

  val submitRegSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          TestPAYERegSetupForm.form.bindFromRequest.fold(
            errors =>
              Future.successful(BadRequest(views.html.pages.test.payeRegistrationSetup(errors, profile.registrationID))),
            success => testPAYERegConnector.addPAYERegistration(success) map {
              case DownstreamOutcome.Success => Ok("PAYE Registration set up successfully")
              case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Registration")
            }
          )
        }
  }

  val regSetupCompanyDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence) {
    implicit user =>
      implicit request =>
        Ok(views.html.pages.test.payeRegCompanyDetailsSetup(TestPAYERegCompanyDetailsSetupForm.form))
  }

  val submitRegSetupCompanyDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          TestPAYERegCompanyDetailsSetupForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.pages.test.payeRegCompanyDetailsSetup(errors))),
            success => testPAYERegConnector.addTestCompanyDetails(success, profile.registrationID) map {
              case DownstreamOutcome.Success => Ok("Company details successfully set up")
              case DownstreamOutcome.Failure => InternalServerError("Error setting up Company Details")
            }
          )
        }
  }

  val regSetupPAYEContact = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegPAYEContactSetup(TestPAYEContactForm.form)))
  }

  val submitRegSetupPAYEContact = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      TestPAYEContactForm.form.bindFromRequest.fold(
        errors => Future.successful(BadRequest(views.html.pages.test.payeRegPAYEContactSetup(errors))),
        success => testPAYERegConnector.addTestPAYEContact(success, profile.registrationID) map {
          case DownstreamOutcome.Success => Ok("PAYE Contact details successfully set up")
          case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Contact details")
        }
      )
    }
  }
}
