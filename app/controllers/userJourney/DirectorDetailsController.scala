/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import forms.directorDetails.DirectorDetailsForm
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.{directorDetails => DirectorDetailsPage}

import scala.concurrent.ExecutionContext

class DirectorDetailsControllerImpl @Inject()(val directorDetailsService: DirectorDetailsService,
                                              val keystoreConnector: KeystoreConnector,
                                              val s4LService: S4LService,
                                              val companyDetailsService: CompanyDetailsService,
                                              val incorpInfoService: IncorporationInformationService,
                                              val authConnector: AuthConnector,
                                              val incorporationInformationConnector: IncorporationInformationConnector,
                                              val payeRegistrationService: PAYERegistrationService,
                                              mcc: MessagesControllerComponents
                                             )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends DirectorDetailsController(mcc) with AuthRedirectUrls

abstract class DirectorDetailsController(mcc: MessagesControllerComponents) extends PayeBaseController(mcc) {
  implicit val appConfig: AppConfig
  implicit val ec: ExecutionContext
  val directorDetailsService: DirectorDetailsService

  def directorDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      directorDetailsService.getDirectorDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map { directors =>
        val ninos = directorDetailsService.createDirectorNinos(directors)
        val names = directorDetailsService.createDisplayNamesMap(directors)
        Ok(DirectorDetailsPage(DirectorDetailsForm.form.fill(ninos), names))
      } recover {
        case _ => InternalServerError(views.html.pages.error.restart())
      }
  }

  def submitDirectorDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
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
