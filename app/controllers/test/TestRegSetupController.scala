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
import com.google.inject.{Inject, Singleton}
import config.{PAYEShortLivedCache, FrontendAuthConnector, PAYESessionCache}
import connectors.{S4LConnector, KeystoreConnector, PAYERegistrationConnector}
import connectors.test.TestPAYERegConnector
import enums.DownstreamOutcome
import forms.test.{TestPAYERegCompanyDetailsSetupForm, TestPAYERegSetupForm}
import services.{S4LService, CommonService, PAYERegistrationService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

//object TestRegSetupController extends TestRegSetupController {
//  //$COVERAGE-OFF$
//  override val authConnector = FrontendAuthConnector
//  override val payeRegService = new PAYERegistrationService(new KeystoreConnector(new PAYESessionCache), new PAYERegistrationConnector(), new S4LService(new S4LConnector(new PAYEShortLivedCache()), new KeystoreConnector(new PAYESessionCache())))
//  override val testPAYERegConnector = new TestPAYERegConnector(new KeystoreConnector(new PAYESessionCache), new PAYERegistrationConnector())
//  override val keystoreConnector = new KeystoreConnector(new PAYESessionCache)
//  //$COVERAGE-ON$
//}
@Singleton
class TestRegSetupController @Inject()(
                                        injPayeRegService:PAYERegistrationService,
                                        injTestPAYERegConnector: TestPAYERegConnector,
                                        injKeystoreConnector: KeystoreConnector)
  extends TestRegSetupCtrl {
  val authConnector = FrontendAuthConnector
  val payeRegService = injPayeRegService
  val testPAYERegConnector = injTestPAYERegConnector
  val keystoreConnector = injKeystoreConnector
}

trait TestRegSetupCtrl extends FrontendController with Actions with CommonService {

  val payeRegService: PAYERegistrationService
  val testPAYERegConnector: TestPAYERegConnector

  val regTeardown = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    testPAYERegConnector.testRegistrationTeardown() map {
      case DownstreamOutcome.Success => Ok("Registration collection successfully cleared")
      case DownstreamOutcome.Failure => InternalServerError("Error clearing registration collection")
    }
  }

  val regSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    for {
      regID <- fetchRegistrationID
    } yield Ok(views.html.pages.test.payeRegistrationSetup(TestPAYERegSetupForm.form, regID))
  }

  val submitRegSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    TestPAYERegSetupForm.form.bindFromRequest.fold (
      errors => for {
        regID <- fetchRegistrationID
      } yield BadRequest(views.html.pages.test.payeRegistrationSetup(errors, regID)),

      success => testPAYERegConnector.addPAYERegistration(success) map {
        case DownstreamOutcome.Success => Ok("PAYE Registration et up successfully")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up PAYE Registration")
      }
    )
  }

  val regSetupCompanyDetails = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    Future.successful(Ok(views.html.pages.test.payeRegCompanyDetailsSetup(TestPAYERegCompanyDetailsSetupForm.form)))
  }

  val submitRegSetupCompanyDetails = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    TestPAYERegCompanyDetailsSetupForm.form.bindFromRequest.fold (
      errors => Future.successful(Ok(views.html.pages.test.payeRegCompanyDetailsSetup(errors))),
      success => testPAYERegConnector.addTestCompanyDetails(success) map {
        case DownstreamOutcome.Success => Ok("Company details successfully set up")
        case DownstreamOutcome.Failure => InternalServerError("Error setting up Company Details")
      }
    )
  }

}
