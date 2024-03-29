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

package controllers.test

import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import models.Address
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestAddressLookupController @Inject()(val companyDetailsService: CompanyDetailsService,
                                                val keystoreConnector: KeystoreConnector,
                                                val payeContactService: PAYEContactService,
                                                val authConnector: AuthConnector,
                                                val s4LService: S4LService,
                                                val incorpInfoService: IncorporationInformationService,
                                                val prepopService: PrepopulationService,
                                                val incorporationInformationConnector: IncorporationInformationConnector,
                                                val payeRegistrationService: PAYERegistrationService,
                                                mcc: MessagesControllerComponents
                                               )(val appConfig: AppConfig, implicit val ec: ExecutionContext)  extends PayeBaseController(mcc)  with AuthRedirectUrls {

  def noLookupPPOBAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>profile =>
    val address = Address(
      line1     = "13 Test Street",
      line2     = "No Lookup Town",
      line3     = Some("NoLookupShire"),
      line4     = None,
      postCode  = None,
      country   = Some("UK")
    )
    for {
      res <- companyDetailsService.submitPPOBAddr(address, profile.registrationID, profile.companyTaxRegistration.transactionId)
      _   <- prepopService.saveAddress(profile.registrationID, address)
    } yield res match {
      case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails)
      case DownstreamOutcome.Failure => InternalServerError("Couldn't save mock PPOB Address")
    }
  }

  def noLookupCorrespondenceAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    val address = Address(
      line1     = "13 Correspondence Street",
      line2     = "No Lookup Town",
      line3     = Some("NoLookupShire"),
      line4     = None,
      postCode  = Some("TE3 3NL"),
      country   = None
    )
    for {
      res <- payeContactService.submitCorrespondence(profile.registrationID, address)
      _   <- prepopService.saveAddress(profile.registrationID, address)
    } yield res match {
      case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.SummaryController.summary)
      case DownstreamOutcome.Failure => InternalServerError("Couldn't save mock Correspondence Address")
    }
  }
}
