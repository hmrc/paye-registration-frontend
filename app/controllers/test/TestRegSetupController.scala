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

import auth.test.TestPAYERegime
import javax.inject.{Inject, Singleton}
import config.FrontendAuthConnector
import connectors.KeystoreConnector
import connectors.test.{TestPAYERegConnect, TestPAYERegConnector}
import enums.DownstreamOutcome
import forms.test.{TestPAYEContactForm, TestPAYERegCompanyDetailsSetupForm, TestPAYERegSetupForm}
import services.{CommonService, PAYERegistrationService, PAYERegistrationSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.Future

@Singleton
class TestRegSetupController @Inject()(injPayeRegService:PAYERegistrationService,
                                       injTestPAYERegConnector: TestPAYERegConnector,
                                       injMessagesApi: MessagesApi)
  extends TestRegSetupCtrl {
  val authConnector = FrontendAuthConnector
  val payeRegService = injPayeRegService
  val testPAYERegConnector = injTestPAYERegConnector
  val messagesApi = injMessagesApi
}

trait TestRegSetupCtrl extends FrontendController with Actions with I18nSupport {

  val payeRegService: PAYERegistrationSrv
  val testPAYERegConnector: TestPAYERegConnect

  val regTeardown = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        testPAYERegConnector.testRegistrationTeardown() map {
          case DownstreamOutcome.Success => Ok("Registration collection successfully cleared")
          case DownstreamOutcome.Failure => InternalServerError("Error clearing registration collection")
        }
  }

  val regSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          regID <- payeRegService.fetchRegistrationID
        } yield Ok(views.html.pages.test.payeRegistrationSetup(TestPAYERegSetupForm.form, regID))
  }

  val submitRegSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        TestPAYERegSetupForm.form.bindFromRequest.fold (
          errors => for {
            regID <- payeRegService.fetchRegistrationID
          } yield BadRequest(views.html.pages.test.payeRegistrationSetup(errors, regID)),

          success => testPAYERegConnector.addPAYERegistration(success) map {
            case DownstreamOutcome.Success => Ok("PAYE Registration set up successfully")
            case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Registration")
          }
        )
  }

  val regSetupCompanyDetails = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.pages.test.payeRegCompanyDetailsSetup(TestPAYERegCompanyDetailsSetupForm.form)))
  }

  val submitRegSetupCompanyDetails = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        TestPAYERegCompanyDetailsSetupForm.form.bindFromRequest.fold (
          errors => Future.successful(BadRequest(views.html.pages.test.payeRegCompanyDetailsSetup(errors))),
          success => testPAYERegConnector.addTestCompanyDetails(success) map {
            case DownstreamOutcome.Success => Ok("Company details successfully set up")
            case DownstreamOutcome.Failure => InternalServerError("Error setting up Company Details")
          }
        )
  }

  val regSetupPAYEContact = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegPAYEContactSetup(TestPAYEContactForm.form)))
  }

  val submitRegSetupPAYEContact = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    TestPAYEContactForm.form.bindFromRequest.fold (
      errors => Future.successful(Ok(views.html.pages.test.payeRegPAYEContactSetup(errors))),
      success => testPAYERegConnector.addTestPAYEContact(success) map {
        case DownstreamOutcome.Success => Ok("PAYE Contact details successfully set up")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Contact details")
      }
    )
  }

}
