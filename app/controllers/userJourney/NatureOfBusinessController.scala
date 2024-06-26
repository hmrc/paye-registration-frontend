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

package controllers.userJourney

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import forms.natureOfBuinessDetails.NatureOfBusinessForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.error.restart
import views.html.pages.{natureOfBusiness => NatureOfBusinessPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NatureOfBusinessController @Inject()(val natureOfBusinessService: NatureOfBusinessService,
                                           val keystoreConnector: KeystoreConnector,
                                           val s4LService: S4LService,
                                           val companyDetailsService: CompanyDetailsService,
                                           val incorpInfoService: IncorporationInformationService,
                                           val authConnector: AuthConnector,
                                           val incorporationInformationConnector: IncorporationInformationConnector,
                                           val payeRegistrationService: PAYERegistrationService,
                                           mcc: MessagesControllerComponents,
                                           NatureOfBusinessPage: NatureOfBusinessPage,
                                           restart: restart
                                          )(implicit val appConfig: AppConfig, implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  def natureOfBusiness: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      natureOfBusinessService.getNatureOfBusiness(profile.registrationID) map {
        case Some(model) => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form.fill(model)))
        case None => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form))
      }
  }

  def submitNatureOfBusiness: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      NatureOfBusinessForm.form.bindFromRequest()



















        .fold(
        errors => Future.successful(BadRequest(NatureOfBusinessPage(errors))),
        success => natureOfBusinessService.saveNatureOfBusiness(success, profile.registrationID) map {
          case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.DirectorDetailsController.directorDetails)
          case DownstreamOutcome.Failure => InternalServerError(restart())
        }
      )
  }
}
