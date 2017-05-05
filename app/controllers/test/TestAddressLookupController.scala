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
import enums.DownstreamOutcome
import models.Address
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile

@Singleton
class TestAddressLookupController @Inject()(
                                             injCompanyDetailsService: CompanyDetailsService,
                                             injKeystoreConnector: KeystoreConnector,
                                             injPAYEContactService: PAYEContactService)
  extends TestAddressLookupCtrl {
  val authConnector = FrontendAuthConnector
  val companyDetailsService = injCompanyDetailsService
  val payeContactService = injPAYEContactService
  val keystoreConnector = injKeystoreConnector
}

trait TestAddressLookupCtrl extends FrontendController with Actions with SessionProfile {
  val companyDetailsService: CompanyDetailsSrv
  val payeContactService: PAYEContactSrv
  val keystoreConnector: KeystoreConnect

  val noLookupPPOBAddress = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          companyDetailsService.submitPPOBAddr(
            Address(
              line1 = "13 Test Street",
              line2 = "No Lookup Town",
              line3 = Some("NoLookupShire"),
              line4 = None,
              postCode = None,
              country = Some("UK")
            ),
            profile.registrationID,
            profile.companyTaxRegistration.transactionId
          ) map {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            case DownstreamOutcome.Failure => InternalServerError("Couldn't save mock PPOB Address")
          }
        }
  }

  val noLookupCorrespondenceAddress = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          payeContactService.submitCorrespondence(
            Address(
              line1 = "13 Correspondence Street",
              line2 = "No Lookup Town",
              line3 = Some("NoLookupShire"),
              line4 = None,
              postCode = Some("TE3 3NL"),
              country = None
            ),
            profile.registrationID
          ) map {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.SummaryController.summary())
            case DownstreamOutcome.Failure => InternalServerError("Couldn't save mock PPOB Address")
          }
        }
  }
}
