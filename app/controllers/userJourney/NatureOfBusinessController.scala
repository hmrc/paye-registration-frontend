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
import enums.DownstreamOutcome
import forms.natureOfBuinessDetails.NatureOfBusinessForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CompanyDetailsService, CompanyDetailsSrv, NatureOfBusinessService, NatureOfBusinessSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.{natureOfBusiness => NatureOfBusinessPage}

@Singleton
class NatureOfBusinessController @Inject()(injMessagesApi: MessagesApi,
                                           injNatureOfBusinessService: NatureOfBusinessService,
                                           injKeystoreConnector: KeystoreConnector,
                                           injPayeRegistrationConnector: PAYERegistrationConnector,
                                           injCompanyDetailsService: CompanyDetailsService) extends NatureOfBusinessCtrl {
  val authConnector = FrontendAuthConnector
  implicit val messagesApi = injMessagesApi
  val natureOfBusinessService = injNatureOfBusinessService
  val companyDetailsService = injCompanyDetailsService
  val keystoreConnector = injKeystoreConnector
  val payeRegistrationConnector = injPayeRegistrationConnector
}

trait NatureOfBusinessCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val authConnector: AuthConnector
  implicit val messagesApi: MessagesApi
  val natureOfBusinessService: NatureOfBusinessSrv
  val companyDetailsService: CompanyDetailsSrv
  val keystoreConnector: KeystoreConnect

  val natureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            nob <- natureOfBusinessService.getNatureOfBusiness(profile.registrationID)
          } yield nob match {
            case Some(model) => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form.fill(model), companyDetails.companyName))
            case None => Ok(NatureOfBusinessPage(NatureOfBusinessForm.form, companyDetails.companyName))
          }
        }
  }

  val submitNatureOfBusiness: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          NatureOfBusinessForm.form.bindFromRequest.fold(
            errors => {
              companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) map {
                details =>
                  BadRequest(NatureOfBusinessPage(errors, details.companyName))
              }
            },
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
