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
import forms.payeContactDetails.{CorrespondenceAddressForm, PAYEContactDetailsForm}
import models.view._
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.SessionProfile
import views.html.pages.payeContact.{correspondenceAddress => PAYECorrespondenceAddressPage, payeContactDetails => PAYEContactDetailsPage}
import common.exceptions.DownstreamExceptions.{PPOBAddressNotFoundException, S4LFetchException}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class PAYEContactController @Inject()(injCompanyDetailsService: CompanyDetailsService,
                                      injPAYEContactService: PAYEContactService,
                                      injAddressLookupService: AddressLookupService,
                                      injKeystoreConnector: KeystoreConnector,
                                      injPayeRegistrationConnector: PAYERegistrationConnector,
                                      injMessagesApi: MessagesApi,
                                      injPrepopulationService: PrepopulationService) extends PAYEContactCtrl {
  val authConnector = FrontendAuthConnector
  val companyDetailsService = injCompanyDetailsService
  val payeContactService = injPAYEContactService
  val addressLookupService = injAddressLookupService
  val keystoreConnector = injKeystoreConnector
  val messagesApi = injMessagesApi
  val payeRegistrationConnector = injPayeRegistrationConnector
  val prepopService = injPrepopulationService
}

trait PAYEContactCtrl extends FrontendController with Actions with I18nSupport with SessionProfile {

  val companyDetailsService: CompanyDetailsSrv
  val payeContactService: PAYEContactSrv
  val addressLookupService: AddressLookupSrv
  val keystoreConnector: KeystoreConnect
  val prepopService: PrepopulationSrv

  val payeContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            payeContact    <- payeContactService.getPAYEContact(profile.registrationID)
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
            success => {
              val trimmed = trimPAYEContactDetails(success)
              payeContactService.submitPayeContactDetails(profile.registrationID, trimmed) map {
                case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
                case DownstreamOutcome.Success => Redirect(routes.PAYEContactController.payeCorrespondenceAddress())
              }
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
            prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, payeContact.correspondenceAddress)
          } yield {
            val addressMap = payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
            Ok(PAYECorrespondenceAddressPage(CorrespondenceAddressForm.form.fill(ChosenAddress(CorrespondenceAddress)),
                                            addressMap.get("ro"),
                                            addressMap.get("ppob"),
                                            addressMap.get("correspondence"),
                                            prepopAddresses))
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
              prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, payeContact.correspondenceAddress)
            } yield {
              val addressMap = payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
              BadRequest(PAYECorrespondenceAddressPage(errs, addressMap.get("ro"), addressMap.get("ppob"), addressMap.get("correspondence"), prepopAddresses))
            },
            success => submitCorrespondenceAddress(profile.registrationID, profile.companyTaxRegistration.transactionId, success.chosenAddress) flatMap {
              case DownstreamOutcome.Success => Future.successful(Redirect(controllers.userJourney.routes.SummaryController.summary()))
              case DownstreamOutcome.Failure => Future.successful(InternalServerError(views.html.pages.error.restart()))
              case DownstreamOutcome.Redirect => addressLookupService.buildAddressLookupUrl("payereg2", controllers.userJourney.routes.PAYEContactController.savePAYECorrespondenceAddress()) map {
                redirectUrl => Redirect(redirectUrl)
              }
            }
          )
        }
  }

  private def submitCorrespondenceAddress(regId: String, txId: String, choice: AddressChoice)(implicit user: AuthContext, hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    choice match {
      case CorrespondenceAddress =>
        Future.successful(DownstreamOutcome.Success)
      case ROAddress => submitCorrespondenceWithROAddress(regId, txId)
      case prepop: PrepopAddress => submitCorrespondenceWithPrepopAddress(regId, prepop)
      case PPOBAddress => submitCorrespondenceWithPPOBAddress(regId, txId)
      case Other =>
        Future.successful(DownstreamOutcome.Redirect)
    }
  }

  private def submitCorrespondenceWithROAddress(regId: String, txId: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      companyDetails <- companyDetailsService.getCompanyDetails(regId, txId)
      res <- payeContactService.submitCorrespondence(regId, companyDetails.roAddress)
      _ <- payeContactService.auditCorrespondenceAddress(regId, "RegisteredOffice")
    } yield res
  }

  private def submitCorrespondenceWithPPOBAddress(regId: String, txId: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    (for {
      companyDetails <- companyDetailsService.getCompanyDetails(regId, txId)
      res <- payeContactService.submitCorrespondence(regId, companyDetails.ppobAddress.getOrElse(throw new PPOBAddressNotFoundException))
      _ <- payeContactService.auditCorrespondenceAddress(regId, "PrincipalPlaceOfBusiness")
    } yield res) recover {
      case _: PPOBAddressNotFoundException =>
        Logger.warn(s"[PAYEContactService] [submitCorrespondenceWithPPOBAddress] - Error while saving Correspondence Address with a PPOBAddress which is missing")
        DownstreamOutcome.Failure
    }
  }

  private def submitCorrespondenceWithPrepopAddress(regId: String, prepop: PrepopAddress)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    (for {
      prepopAddress <- prepopService.getAddress(regId, prepop.index)
      res <- payeContactService.submitCorrespondence(regId, prepopAddress)
    } yield res) recover {
      case e: S4LFetchException =>
        Logger.warn(s"[PAYEContactService] [submitCorrespondenceWithPrepopAddress] - Error while saving Correspondence Address with a PrepopAddress: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

  val savePAYECorrespondenceAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            Some(address) <- addressLookupService.getAddress
            res <- payeContactService.submitCorrespondence(profile.registrationID, address )
            _ <- prepopService.saveAddress(profile.registrationID, address)
          } yield res match {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.SummaryController.summary())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        }
  }

  private def trimPAYEContactDetails(details: PAYEContactDetails) = details.copy(
    digitalContactDetails = details.digitalContactDetails.copy(
      email         = details.digitalContactDetails.email map(_.trim),
      phoneNumber   = details.digitalContactDetails.phoneNumber map(_.trim),
      mobileNumber  = details.digitalContactDetails.mobileNumber map(_.trim)
    )
  )
}
