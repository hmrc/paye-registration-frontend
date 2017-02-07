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
import play.api.i18n.{I18nSupport, MessagesApi}
import services.{AddressLookupService, AddressLookupSrv}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class AddressLookupController @Inject()(
                                         injAddressLookupService: AddressLookupService,
                                         injMessagesApi: MessagesApi)
  extends AddressLookupCtrl{
  val authConnector = FrontendAuthConnector
  val addressLookupService = injAddressLookupService
  val messagesApi = injMessagesApi
}

trait AddressLookupCtrl extends FrontendController with Actions with I18nSupport {
  val addressLookupService : AddressLookupSrv

  val redirectToLookup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(addressLookupService.buildAddressLookupUrl()))
  }

  val returnFromLookup = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        request.getQueryString("id") match {
          case Some(id) => addressLookupService.getAddress(id)
        }
        Future.successful(Redirect(controllers.userJourney.routes.EmploymentController.employingStaff()))
  }

}
