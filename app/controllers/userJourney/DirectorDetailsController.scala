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

import javax.inject.Inject

import connectors.KeystoreConnector
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.directorDetails.DirectorDetailsForm
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CompanyDetailsService, DirectorDetailsService, IncorporationInformationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.{directorDetails => DirectorDetailsPage}

class DirectorDetailsControllerImpl @Inject()(val messagesApi: MessagesApi,
                                              val directorDetailsService: DirectorDetailsService,
                                              val keystoreConnector: KeystoreConnector,
                                              val config: Configuration,
                                              val s4LService: S4LService,
                                              val companyDetailsService: CompanyDetailsService,
                                              val incorpInfoService: IncorporationInformationService,
                                              val authConnector: AuthConnector) extends DirectorDetailsController with AuthRedirectUrls

trait DirectorDetailsController extends PayeBaseController {
  val directorDetailsService : DirectorDetailsService

  def directorDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    directorDetailsService.getDirectorDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map { directors =>
      val ninos = directorDetailsService.createDirectorNinos(directors)
      val names = directorDetailsService.createDisplayNamesMap(directors)
      Ok(DirectorDetailsPage(DirectorDetailsForm.form.fill(ninos), names))
    } recover {
      case _ => InternalServerError(views.html.pages.error.restart())
    }
  }

  def submitDirectorDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    DirectorDetailsForm.form.bindFromRequest.fold(
      errors => directorDetailsService.getDirectorDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map { directors =>
        val names = directorDetailsService.createDisplayNamesMap(directors)
        BadRequest(DirectorDetailsPage(errors, names))
      },
      success => directorDetailsService.submitNinos(success, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
        _ => Redirect(routes.PAYEContactController.payeContactDetails())
      }
    )
  }
}
