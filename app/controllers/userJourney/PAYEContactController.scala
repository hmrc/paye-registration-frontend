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
import common.exceptions.DownstreamExceptions.{PPOBAddressNotFoundException, S4LFetchException}
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import forms.payeContactDetails.{CorrespondenceAddressForm, PAYEContactDetailsForm}
import models.external.AuditingInformation
import models.view._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.api.{Configuration, Logger}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.payeContact.{correspondenceAddress => PAYECorrespondenceAddressPage, payeContactDetails => PAYEContactDetailsPage}

import scala.concurrent.Future

class PAYEContactControllerImpl @Inject()(val companyDetailsService: CompanyDetailsService,
                                          val payeContactService: PAYEContactService,
                                          val addressLookupService: AddressLookupService,
                                          val keystoreConnector: KeystoreConnector,
                                          val messagesApi: MessagesApi,
                                          val authConnector: AuthConnector,
                                          val config: Configuration,
                                          val prepopService: PrepopulationService,
                                          val s4LService: S4LService,
                                          val incorpInfoService: IncorporationInformationService,
                                          val auditService: AuditService,
                                          val incorporationInformationConnector: IncorporationInformationConnector,
                                          val payeRegistrationService: PAYERegistrationService) extends PAYEContactController with AuthRedirectUrls

trait PAYEContactController extends PayeBaseController {

  val companyDetailsService: CompanyDetailsService
  val payeContactService: PAYEContactService
  val addressLookupService: AddressLookupService
  val keystoreConnector: KeystoreConnector
  val prepopService: PrepopulationService
  val auditService: AuditService

  def payeContactDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    payeContactService.getPAYEContact(profile.registrationID) map {
      case PAYEContact(Some(contactDetails), _) => Ok(PAYEContactDetailsPage(PAYEContactDetailsForm.form.fill(contactDetails)))
      case _                                    => Ok(PAYEContactDetailsPage(PAYEContactDetailsForm.form))
    }
  }

  def submitPAYEContactDetails: Action[AnyContent] = isAuthorisedWithProfileAndAuditing { implicit request => profile => implicit audit =>
    PAYEContactDetailsForm.form.bindFromRequest.fold(
      errs    => Future.successful(BadRequest(PAYEContactDetailsPage(errs))),
      success => {
        val trimmed = trimPAYEContactDetails(success)
        payeContactService.submitPayeContactDetails(profile.registrationID, trimmed) map {
          case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          case DownstreamOutcome.Success => Redirect(routes.PAYEContactController.payeCorrespondenceAddress())
        }
      }
    )
  }

  def payeCorrespondenceAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    for {
      payeContact     <- payeContactService.getPAYEContact(profile.registrationID)
      companyDetails  <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
      prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, payeContact.correspondenceAddress)
      addressMap      = payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
    } yield {
      Ok(PAYECorrespondenceAddressPage(
        CorrespondenceAddressForm.form.fill(
          ChosenAddress(CorrespondenceAddress)),
        addressMap.get("ro"),
        addressMap.get("ppob"),
        addressMap.get("correspondence"),
        prepopAddresses
      ))
    }
  }

  def submitPAYECorrespondenceAddress: Action[AnyContent] = isAuthorisedWithProfileAndAuditing { implicit request => profile => implicit audit =>
    CorrespondenceAddressForm.form.bindFromRequest.fold(
      errs => for {
        payeContact     <- payeContactService.getPAYEContact(profile.registrationID)
        companyDetails  <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
        prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, payeContact.correspondenceAddress)
        addressMap      =  payeContactService.getCorrespondenceAddresses(payeContact.correspondenceAddress, companyDetails)
      } yield {
        BadRequest(PAYECorrespondenceAddressPage(errs, addressMap.get("ro"), addressMap.get("ppob"), addressMap.get("correspondence"), prepopAddresses))
      },
      success => submitCorrespondenceAddress(profile.registrationID, profile.companyTaxRegistration.transactionId, success.chosenAddress) flatMap {
        case DownstreamOutcome.Success  => Future.successful(Redirect(controllers.userJourney.routes.SummaryController.summary()))
        case DownstreamOutcome.Failure  => Future.successful(InternalServerError(views.html.pages.error.restart()))
        case DownstreamOutcome.Redirect => addressLookupService.buildAddressLookupUrl("correspondence", controllers.userJourney.routes.PAYEContactController.savePAYECorrespondenceAddress()) map {
          Redirect(_)
        }
      }
    )
  }

  private def submitCorrespondenceAddress(regId: String, txId: String, choice: AddressChoice)
                                         (implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    choice match {
      case CorrespondenceAddress  => Future.successful(DownstreamOutcome.Success)
      case ROAddress              => submitCorrespondenceWithROAddress(regId, txId)
      case prepop: PrepopAddress  => submitCorrespondenceWithPrepopAddress(regId, prepop)
      case PPOBAddress            => submitCorrespondenceWithPPOBAddress(regId, txId)
      case Other                  => Future.successful(DownstreamOutcome.Redirect)
    }
  }

  private def submitCorrespondenceWithROAddress(regId: String, txId: String)(implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    for {
      companyDetails <- companyDetailsService.getCompanyDetails(regId, txId)
      res            <- payeContactService.submitCorrespondence(regId, companyDetails.roAddress)
      _              <- auditService.auditCorrespondenceAddress(regId, "RegisteredOffice")
    } yield res
  }

  private def submitCorrespondenceWithPPOBAddress(regId: String, txId: String)
                                                 (implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    (for {
      companyDetails <- companyDetailsService.getCompanyDetails(regId, txId)
      res            <- payeContactService.submitCorrespondence(regId, companyDetails.ppobAddress.getOrElse(throw new PPOBAddressNotFoundException))
      _              <- auditService.auditCorrespondenceAddress(regId, "PrincipalPlaceOfBusiness")
    } yield res) recover {
      case _: PPOBAddressNotFoundException =>
        Logger.warn(s"[PAYEContactService] [submitCorrespondenceWithPPOBAddress] - Error while saving Correspondence Address with a PPOBAddress which is missing")
        DownstreamOutcome.Failure
    }
  }

  private def submitCorrespondenceWithPrepopAddress(regId: String, prepop: PrepopAddress)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    (for {
      prepopAddress <- prepopService.getAddress(regId, prepop.index)
      res           <- payeContactService.submitCorrespondence(regId, prepopAddress)
    } yield res) recover {
      case e: S4LFetchException =>
        Logger.warn(s"[PAYEContactService] [submitCorrespondenceWithPrepopAddress] - Error while saving Correspondence Address with a PrepopAddress: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

  def savePAYECorrespondenceAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request => profile =>
    for {
      Some(address) <- addressLookupService.getAddress
      res           <- payeContactService.submitCorrespondence(profile.registrationID, address )
      _             <- prepopService.saveAddress(profile.registrationID, address)
    } yield res match {
      case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.SummaryController.summary())
      case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
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
