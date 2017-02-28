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

package controllers.userJourney

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import config.FrontendAuthConnector
import forms.directorDetails.DirectorDetailsForm
import models.view.{Ninos, UserEnteredNino}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import services.{DirectorDetailsService, DirectorDetailsSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.{directorDetails => DirectorDetailsPage}

import scala.concurrent.Future

@Singleton
class DirectorDetailsController @Inject()(injMessagesApi: MessagesApi,
                                          injDirectorDetailsService: DirectorDetailsService) extends DirectorDetailsCtrl {
  val authConnector = FrontendAuthConnector
  val messagesApi = injMessagesApi
  val directorDetailsService = injDirectorDetailsService
}

trait DirectorDetailsCtrl extends FrontendController with Actions with I18nSupport {

  val directorDetailsService : DirectorDetailsSrv

  val directorDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        directorDetailsService.getDirectorDetails map {
          directors =>
            val ninos = directorDetailsService.createDirectorNinos(directors)
            val names = directorDetailsService.createDisplayNamesMap(directors)
            Ok(DirectorDetailsPage(DirectorDetailsForm.form.fill(ninos), names))
        }
  }

  val submitDirectorDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        DirectorDetailsForm.form.bindFromRequest.fold(
          errors => {
            directorDetailsService.getDirectorDetails map {
              directors =>
                val names = directorDetailsService.createDisplayNamesMap(directors)
                BadRequest(DirectorDetailsPage(errors, names))
            }
          },
          success => {
            directorDetailsService.submitNinos(success) map {
              _ => Redirect(routes.PAYEContactDetailsController.payeContactDetails())
            }
          }
        )
  }
}
