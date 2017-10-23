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
import connectors.test.{TestIncorpInfoConnect, TestIncorpInfoConnector}
import connectors.{BusinessRegistrationConnect, BusinessRegistrationConnector, KeystoreConnect, KeystoreConnector}
import forms.test.TestCoHoCompanyDetailsForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import services.{IncorporationInformationService, IncorporationInformationSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class TestCoHoController @Inject()(val testIncorpInfoConnector: TestIncorpInfoConnector,
                                   val coHoAPIService: IncorporationInformationService,
                                   val keystoreConnector: KeystoreConnector,
                                   val businessRegConnector: BusinessRegistrationConnector,
                                   val messagesApi: MessagesApi) extends TestCoHoCtrl {
  val authConnector = FrontendAuthConnector
}

trait TestCoHoCtrl extends FrontendController with Actions with I18nSupport {
  val testIncorpInfoConnector: TestIncorpInfoConnect
  val businessRegConnector: BusinessRegistrationConnect
  val keystoreConnector: KeystoreConnect
  val coHoAPIService: IncorporationInformationSrv

  def coHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.pages.test.coHoCompanyDetailsSetup(TestCoHoCompanyDetailsForm.form)))
  }

  def submitCoHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        TestCoHoCompanyDetailsForm.form.bindFromRequest.fold(
          errors  => Future.successful(BadRequest(views.html.pages.test.coHoCompanyDetailsSetup(errors))),
          success => for {
            profile <- businessRegConnector.retrieveCurrentProfile
            res     <- doAddCoHoCompanyDetails(profile.registrationID, success.companyName)
          } yield Ok(res)
        )
  }

  def coHoCompanyDetailsTearDown = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          profile <- businessRegConnector.retrieveCurrentProfile
          res     <- doCoHoCompanyDetailsTearDown(profile.registrationID)
        } yield Ok(res)
  }

  protected[controllers] def doCoHoCompanyDetailsTearDown(regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    testIncorpInfoConnector.teardownIndividualCoHoCompanyDetails(regId).map (_ =>"Company details collection removed")
  }

  protected[controllers] def doAddCoHoCompanyDetails(regId: String, companyName: String)(implicit request: Request[AnyContent]): Future[String] = {
    for {
      resp <- testIncorpInfoConnector.setupCoHoCompanyDetails(regId, companyName)
    } yield s"Company Name: $companyName, registration ID: $regId. Response status: ${resp.status}"
  }


}
