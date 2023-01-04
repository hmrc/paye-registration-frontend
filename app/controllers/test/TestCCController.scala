/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import connectors.test.TestBusinessRegConnector
import connectors.{IncorporationInformationConnector, KeystoreConnector, PAYERegistrationConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.test.TestCCUpdateForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.test.updateCCPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestCCController @Inject()(val testBusRegConnector: TestBusinessRegConnector,
                                 val keystoreConnector: KeystoreConnector,
                                 val authConnector: AuthConnector,
                                 val s4LService: S4LService,
                                 val companyDetailsService: CompanyDetailsService,
                                 val incorpInfoService: IncorporationInformationService,
                                 val payeRegistrationConnector: PAYERegistrationConnector,
                                 val incorporationInformationConnector: IncorporationInformationConnector,
                                 val payeRegistrationService: PAYERegistrationService,
                                 mcc: MessagesControllerComponents,
                                 updateCCPage: updateCCPage
                                )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  def showUpdateCC: Action[AnyContent] = isAuthorised { implicit request =>
    Future.successful(Ok(updateCCPage(TestCCUpdateForm.form)))
  }

  def submitUpdateCC: Action[AnyContent] = isAuthorisedWithProfile { implicit user =>
    profile =>
      TestCCUpdateForm.form.bindFromRequest.fold(
        errors => Future.successful(BadRequest(updateCCPage(errors))),
        valid => testBusRegConnector.updateCompletionCapacity(profile.registrationID, valid.cc) map (_ => Ok(valid.cc))
      )
  }
}
