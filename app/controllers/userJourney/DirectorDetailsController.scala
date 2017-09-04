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
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnector}
import forms.directorDetails.DirectorDetailsForm
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{DirectorDetailsService, DirectorDetailsSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.{directorDetails => DirectorDetailsPage}

@Singleton
class DirectorDetailsController @Inject()(val messagesApi: MessagesApi,
                                          val directorDetailsService: DirectorDetailsService,
                                          val keystoreConnector: KeystoreConnector,
                                          val payeRegistrationConnector: PAYERegistrationConnector) extends DirectorDetailsCtrl {
  val authConnector = FrontendAuthConnector
}

trait DirectorDetailsCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val directorDetailsService : DirectorDetailsSrv
  val keystoreConnector : KeystoreConnect

  val directorDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          directorDetailsService.getDirectorDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map { directors =>
            val ninos = directorDetailsService.createDirectorNinos(directors)
            val names = directorDetailsService.createDisplayNamesMap(directors)
            Ok(DirectorDetailsPage(DirectorDetailsForm.form.fill(ninos), names))
          }recover {
            case _ => InternalServerError(views.html.pages.error.restart())
          }
        }
  }

  val submitDirectorDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          DirectorDetailsForm.form.bindFromRequest.fold(
            errors => {
              directorDetailsService.getDirectorDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map {
                directors =>
                  val names = directorDetailsService.createDisplayNamesMap(directors)
                  BadRequest(DirectorDetailsPage(errors, names))
              }
            },
            success => {
              directorDetailsService.submitNinos(success, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
                _ => Redirect(routes.PAYEContactController.payeContactDetails())
              }
            }
          )
        }
  }
}
