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
import connectors.test.TestIncorpInfoConnector
import connectors.{BusinessRegistrationConnector, IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.test.TestCoHoCompanyDetailsForm
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class TestCoHoControllerImpl @Inject()(val testIncorpInfoConnector: TestIncorpInfoConnector,
                                       val coHoAPIService: IncorporationInformationService,
                                       val keystoreConnector: KeystoreConnector,
                                       val businessRegConnector: BusinessRegistrationConnector,
                                       val authConnector: AuthConnector,
                                       val s4LService: S4LService,
                                       val config: Configuration,
                                       val companyDetailsService: CompanyDetailsService,
                                       val incorpInfoService: IncorporationInformationService,
                                       val messagesApi: MessagesApi,
                                       val incorporationInformationConnector: IncorporationInformationConnector,
                                       val payeRegistrationService: PAYERegistrationService) extends TestCoHoController with AuthRedirectUrls

trait TestCoHoController extends PayeBaseController {
  val testIncorpInfoConnector: TestIncorpInfoConnector
  val businessRegConnector: BusinessRegistrationConnector
  val keystoreConnector: KeystoreConnector
  val coHoAPIService: IncorporationInformationService

  def coHoCompanyDetailsSetup = isAuthorised { implicit request =>
    Future.successful(Ok(views.html.pages.test.coHoCompanyDetailsSetup(TestCoHoCompanyDetailsForm.form)))
  }

  def submitCoHoCompanyDetailsSetup: Action[AnyContent] = isAuthorised { implicit request =>
    TestCoHoCompanyDetailsForm.form.bindFromRequest.fold(
      errors  => Future.successful(BadRequest(views.html.pages.test.coHoCompanyDetailsSetup(errors))),
      success => for {
        profile <- businessRegConnector.retrieveCurrentProfile
        res     <- doAddCoHoCompanyDetails(profile.registrationID, success.companyName)
      } yield Ok(res)
    )
  }

  def coHoCompanyDetailsTearDown: Action[AnyContent] = isAuthorised { implicit request =>
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
