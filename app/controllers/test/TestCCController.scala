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
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import connectors.test.{TestBusinessRegConnect, TestBusinessRegConnector}
import forms.test.TestCCUpdateForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.test.updateCCPage

import scala.concurrent.Future

@Singleton
class TestCCController @Inject()(injMessagesApi: MessagesApi, injTestBusinessRegConnect: TestBusinessRegConnector,
                                 injKeystoreConnector: KeystoreConnector, injPayeRegConnector: PAYERegistrationConnector) extends TestCCCtrl{
  val authConnector = FrontendAuthConnector
  val messagesApi = injMessagesApi
  val testBusRegConnector = injTestBusinessRegConnect
  val keystoreConnector = injKeystoreConnector
  val payeRegistrationConnector = injPayeRegConnector
}

trait TestCCCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val testBusRegConnector: TestBusinessRegConnect

  def showUpdateCC: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(updateCCPage(TestCCUpdateForm.form)))
  }

  def submitUpdateCC: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          TestCCUpdateForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(updateCCPage(errors))),
            valid => testBusRegConnector.updateCompletionCapacity(profile.registrationID, valid.cc) map(_ => Ok(valid.cc))
          )
        }
  }
}
