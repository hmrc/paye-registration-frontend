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
import config.{PAYESessionCache, FrontendAuthConnector}
import connectors.{CoHoAPIConnector, KeystoreConnector}
import connectors.test.TestCoHoAPIConnector
import forms.test.TestCoHoCompanyDetailsForm
import services.CoHoAPIService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

//object TestCoHoController extends TestCoHoController {
//  //$COVERAGE-OFF$
//  override val authConnector = FrontendAuthConnector
//  override val testCoHoAPIConnector = new TestCoHoAPIConnector()
//  override val coHoAPIService = new CoHoAPIService(new KeystoreConnector(new PAYESessionCache()), new CoHoAPIConnector())//  //$COVERAGE-ON$
//}
@Singleton
class TestCoHoController @Inject()(
                                    injTestCoHoAPIConnector: TestCoHoAPIConnector,
                                    injCoHoAPIService: CoHoAPIService)
  extends TestCoHoCtrl {
  val authConnector = FrontendAuthConnector
  val testCoHoAPIConnector = injTestCoHoAPIConnector
  val coHoAPIService = injCoHoAPIService
}

trait TestCoHoCtrl extends FrontendController with Actions {

  val testCoHoAPIConnector: TestCoHoAPIConnector
  val coHoAPIService: CoHoAPIService

  def coHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    Future.successful(Ok(views.html.pages.test.coHoCompanyDetailsSetup(TestCoHoCompanyDetailsForm.form)))
  }

  def submitCoHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    TestCoHoCompanyDetailsForm.form.bindFromRequest.fold(
      errors => Future.successful(Ok(views.html.pages.test.coHoCompanyDetailsSetup(errors))),
      success => for {
        regId <- coHoAPIService.fetchRegistrationID
        resp <- testCoHoAPIConnector.addCoHoCompanyDetails(success.toCoHoCompanyDetailsAPIModel(regId))
      } yield Ok(s"Company details response status: ${resp.status}")
    )
  }

  def coHoCompanyDetailsTearDown = AuthorisedFor(taxRegime = new TestPAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    testCoHoAPIConnector.tearDownCoHoCompanyDetails().map { result =>
      Ok("Company details collection removed")
    }
  }

}
