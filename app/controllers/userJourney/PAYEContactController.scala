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
import connectors.{KeystoreConnect, KeystoreConnector}
import enums.DownstreamOutcome
import forms.payeContactDetails.{CorrespondenceAddressForm, PAYEContactDetailsForm}
import models.view.PAYEContact
import models.view.{AddressChoice, ChosenAddress}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.payeContact.{correspondenceAddress => PAYECorrespondenceAddressPage, payeContactDetails => PAYEContactDetailsPage}

import scala.concurrent.Future

@Singleton
class PAYEContactController @Inject()(
                                       injCompanyDetailsService: CompanyDetailsService,
                                       injPAYEContactService: PAYEContactService,
                                       injAddressLookupService: AddressLookupService,
                                       injKeystoreConnector: KeystoreConnector,
                                       injMessagesApi: MessagesApi)
  extends PAYEContactCtrl {
  val authConnector = FrontendAuthConnector
  val companyDetailsService = injCompanyDetailsService
  val payeContactService = injPAYEContactService
  val addressLookupService = injAddressLookupService
  val keystoreConnector = injKeystoreConnector
  val messagesApi = injMessagesApi
}

trait PAYEContactCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val companyDetailsService: CompanyDetailsSrv
  val payeContactService: PAYEContactSrv
  val addressLookupService: AddressLookupSrv
  val keystoreConnector: KeystoreConnect

  val payeContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            payeContact <- payeContactService.getPAYEContact(profile.registrationID)
          } yield payeContact match {
            case PAYEContact(Some(contactDetails), _) => Ok(PAYEContactDetailsPage(companyDetails.companyName, PAYEContactDetailsForm.form.fill(contactDetails)))
            case _ => Ok(PAYEContactDetailsPage(companyDetails.companyName, PAYEContactDetailsForm.form))
          }
        }
  }

  val submitPAYEContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          PAYEContactDetailsForm.form.bindFromRequest.fold(
            errs => companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
              .map (details => BadRequest(PAYEContactDetailsPage(details.companyName, errs))),
            success => payeContactService.submitPayeContactDetails(success, profile.registrationID) map {
              case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
              case DownstreamOutcome.Success => Redirect(routes.PAYEContactController.payeCorrespondenceAddress())
            }
          )
        }
  }

  val payeCorrespondenceAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            payeContact <- payeContactService.getPAYEContact(profile.registrationID)
            companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
          } yield {
            val addressMap = payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
            Ok(PAYECorrespondenceAddressPage(CorrespondenceAddressForm.form.fill(ChosenAddress(AddressChoice.correspondenceAddress)), addressMap.get("ro"), addressMap.get("correspondence")))
          }
        }
  }

  val submitPAYECorrespondenceAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          CorrespondenceAddressForm.form.bindFromRequest.fold(
            errs => for {
              payeContact <- payeContactService.getPAYEContact(profile.registrationID)
              companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            } yield {
              val addressMap = payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
              BadRequest(PAYECorrespondenceAddressPage(errs, addressMap.get("ro"), addressMap.get("correspondence")))
            },
            success => success.chosenAddress match {
              case AddressChoice.correspondenceAddress =>
                Future.successful(Redirect(controllers.userJourney.routes.EmploymentController.subcontractors()))
              case AddressChoice.roAddress => for {
                companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
                res <- payeContactService.submitCorrespondence(companyDetails.roAddress, profile.registrationID)
              } yield res match {
                case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
                case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
              }
              case AddressChoice.other =>
                addressLookupService.buildAddressLookupUrl("payereg1", controllers.userJourney.routes.PAYEContactController.savePAYECorrespondenceAddress()) map {
                  redirectUrl => Redirect(redirectUrl)
                }
            }
          )
        }
  }

  val savePAYECorrespondenceAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            Some(address) <- addressLookupService.getAddress
            res <- payeContactService.submitCorrespondence(address, profile.registrationID)
          } yield res match {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.EmploymentController.subcontractors())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        }
  }
}
