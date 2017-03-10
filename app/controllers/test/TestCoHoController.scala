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
import connectors.test.{TestCoHoAPIConnect, TestCoHoAPIConnector}
import forms.test.TestCoHoCompanyDetailsForm
import models.test.CoHoCompanyDetailsFormModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import services.{CoHoAPIService, CoHoAPISrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class TestCoHoController @Inject()(injTestCoHoAPIConnector: TestCoHoAPIConnector,
                                   injCoHoAPIService: CoHoAPIService,
                                   injKeystoreConnector: KeystoreConnector,
                                   injMessagesApi: MessagesApi)
  extends TestCoHoCtrl {
  val authConnector = FrontendAuthConnector
  val testCoHoAPIConnector = injTestCoHoAPIConnector
  val coHoAPIService = injCoHoAPIService
  val keystoreConnector = injKeystoreConnector
  val messagesApi = injMessagesApi
}

trait TestCoHoCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val testCoHoAPIConnector: TestCoHoAPIConnect
  val keystoreConnector : KeystoreConnect
  val coHoAPIService: CoHoAPISrv

  def coHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          Future.successful(Ok(views.html.pages.test.coHoCompanyDetailsSetup(TestCoHoCompanyDetailsForm.form)))
        }
  }

  def submitCoHoCompanyDetailsSetup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          TestCoHoCompanyDetailsForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.pages.test.coHoCompanyDetailsSetup(errors))),
            success => for {
              res <- doAddCoHoCompanyDetails(success, profile.registrationID)
            } yield Ok(res)
          )
        }
  }

  def coHoCompanyDetailsTearDown = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            res <- doCoHoCompanyDetailsTearDown
          } yield Ok(res)
        }
  }

  protected[controllers] def doCoHoCompanyDetailsTearDown(implicit request: Request[AnyContent]): Future[String] = {
    testCoHoAPIConnector.tearDownCoHoCompanyDetails().map (_ =>"Company details collection removed")
  }

  protected[controllers] def doAddCoHoCompanyDetails(formModel: CoHoCompanyDetailsFormModel, regId: String)(implicit request: Request[AnyContent]): Future[String] = {
    for {
      resp <- testCoHoAPIConnector.addCoHoCompanyDetails(formModel.toCoHoCompanyDetailsAPIModel(regId))
    } yield s"Company Name: ${formModel.companyName}, response status: ${resp.status}"
  }

}
