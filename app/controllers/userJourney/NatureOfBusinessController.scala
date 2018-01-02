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

package controllers.userJourney

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnector}
import enums.DownstreamOutcome
import forms.natureOfBuinessDetails.NatureOfBusinessForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{NatureOfBusinessService, NatureOfBusinessSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.{natureOfBusiness => NatureOfBusinessPage}

import scala.concurrent.Future

@Singleton
class NatureOfBusinessController @Inject()(val messagesApi: MessagesApi,
                                           val natureOfBusinessService: NatureOfBusinessService,
                                           val keystoreConnector: KeystoreConnector,
                                           val payeRegistrationConnector: PAYERegistrationConnector) extends NatureOfBusinessCtrl {
  val authConnector = FrontendAuthConnector
}

trait NatureOfBusinessCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val authConnector: AuthConnector
  implicit val messagesApi: MessagesApi
  val natureOfBusinessService: NatureOfBusinessSrv
  val keystoreConnector: KeystoreConnect

  val natureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          natureOfBusinessService.getNatureOfBusiness(profile.registrationID) map {
            case Some(model) => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form.fill(model)))
            case None        => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form))
          }
        }
  }

  val submitNatureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          NatureOfBusinessForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(NatureOfBusinessPage(errors))),
            success => {
              natureOfBusinessService.saveNatureOfBusiness(success, profile.registrationID) map {
                case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.DirectorDetailsController.directorDetails())
                case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
              }
            }
          )
        }
  }
}
