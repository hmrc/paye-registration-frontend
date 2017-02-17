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

import javax.inject.{Singleton, Inject}

import auth.PAYERegime
import config.FrontendAuthConnector
import enums.DownstreamOutcome
import models.view.Address
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class TestAddressLookupController @Inject()(
                                             injCompanyDetailsService: CompanyDetailsService)
  extends TestAddressLookupCtrl {
  val authConnector = FrontendAuthConnector
  val companyDetailsService = injCompanyDetailsService
}

trait TestAddressLookupCtrl extends FrontendController with Actions {
  val companyDetailsService: CompanyDetailsSrv


  val noLookupPPOBAddress = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        companyDetailsService.submitPPOBAddr(
          Address(
            line1 = "13 Test Street",
            line2 = "No Lookup Town",
            line3 = Some("NoLookupShire"),
            line4 = None,
            postCode = Some("TE3 3NL"),
            country = Some("UK")
          )
        ) map {
          case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          case DownstreamOutcome.Failure => InternalServerError("Couldn't save mock PPOB Address")
        }
  }
}