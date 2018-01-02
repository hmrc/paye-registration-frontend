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

import javax.inject.{Inject, Singleton}

import auth.PAYERegime
import common.exceptions.DownstreamExceptions.S4LFetchException
import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector, PAYERegistrationConnector}
import enums.DownstreamOutcome
import forms.companyDetails.{BusinessContactDetailsForm, PPOBForm, TradingNameForm}
import models.view._
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{SessionProfile, UpToDateCompanyDetails}
import views.html.pages.companyDetails.{confirmROAddress, businessContactDetails => BusinessContactDetailsPage, ppobAddress => PPOBAddressPage, tradingName => TradingNamePage}

import scala.concurrent.Future

@Singleton
class CompanyDetailsController @Inject()(val s4LService: S4LService,
                                         val keystoreConnector: KeystoreConnector,
                                         val companyDetailsService: CompanyDetailsService,
                                         val incorpInfoService: IncorporationInformationService,
                                         val messagesApi: MessagesApi,
                                         val payeRegistrationConnector: PAYERegistrationConnector,
                                         val addressLookupService: AddressLookupService,
                                         val prepopService: PrepopulationService,
                                         val auditService: AuditService) extends CompanyDetailsCtrl {
  val authConnector = FrontendAuthConnector
}

trait CompanyDetailsCtrl extends FrontendController with Actions with I18nSupport with SessionProfile with UpToDateCompanyDetails {
  val s4LService: S4LSrv
  val keystoreConnector: KeystoreConnect
  val companyDetailsService: CompanyDetailsSrv
  val incorpInfoService: IncorporationInformationSrv
  val addressLookupService: AddressLookupSrv
  val prepopService: PrepopulationSrv
  val auditService: AuditSrv

  val tradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      withCurrentProfile { profile =>
        withLatestCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) { companyDetails =>
          companyDetails.tradingName match {
            case Some(model)  => Ok(TradingNamePage(TradingNameForm.form.fill(model), companyDetails.companyName))
            case _            => Ok(TradingNamePage(TradingNameForm.form, companyDetails.companyName))
          }
        }
      }
  }

  val submitTradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>
    withCurrentProfile { profile =>
      TradingNameForm.form.bindFromRequest.fold(
        errors => badRequestResponse(profile.registrationID, profile.companyTaxRegistration.transactionId, errors),
        success => {
          val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
          if (validatedForm.hasErrors) {
            badRequestResponse(profile.registrationID, profile.companyTaxRegistration.transactionId, validatedForm)
          } else {
            val trimmedTradingName = success.copy(tradingName = success.tradingName.map(_.trim))
            companyDetailsService.submitTradingName(trimmedTradingName, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
              case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.roAddress())
              case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
            }
          }
        }
      )
    }
  }

  val roAddress : Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          withLatestCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) { companyDetails =>
            Ok(confirmROAddress(companyDetails.companyName, companyDetails.roAddress))
          }
        }
  }

  val confirmRO : Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.ppobAddress()))
  }

  private def badRequestResponse(regId: String, txID: String, form: Form[TradingName])(implicit request: Request[AnyContent]): Future[Result] = {
    companyDetailsService.getCompanyDetails(regId, txID) map {
      details => BadRequest(TradingNamePage(form, details.companyName))
    }
  }

  val businessContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) flatMap {
            details => details.businessContactDetails match {
              case Some(bcd) => Future.successful(Ok(BusinessContactDetailsPage(BusinessContactDetailsForm.form.fill(bcd))))
              case None      => Future.successful(Ok(BusinessContactDetailsPage(BusinessContactDetailsForm.form)))
            }
          }
        }
  }

  val submitBusinessContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          BusinessContactDetailsForm.form.bindFromRequest.fold(
            errs => Future.successful(BadRequest(BusinessContactDetailsPage(errs))),
            success => {
              val trimmed = success.copy(email = success.email map(_.trim), phoneNumber = success.phoneNumber map(_.trim), mobileNumber = success.mobileNumber map(_.trim))
              companyDetailsService.submitBusinessContact(trimmed, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
                case DownstreamOutcome.Success => Redirect(routes.NatureOfBusinessController.natureOfBusiness())
                case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
              }
            }
          )
        }
  }

  val ppobAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, None)
          } yield {
            val addressMap = companyDetailsService.getPPOBPageAddresses(companyDetails)
            Ok(PPOBAddressPage(PPOBForm.form.fill(ChosenAddress(PPOBAddress)), addressMap.get("ro"), addressMap.get("ppob"), prepopAddresses))
          }
        }
  }

  val submitPPOBAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          PPOBForm.form.bindFromRequest.fold(
            errs => for {
              details <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
              prepopAddresses <- prepopService.getPrePopAddresses(profile.registrationID, details.roAddress, details.ppobAddress, None)
            } yield {
              val addressMap = companyDetailsService.getPPOBPageAddresses(details)
              BadRequest(PPOBAddressPage(errs, addressMap.get("ro"), addressMap.get("ppob"), prepopAddresses))
            },
            success => submitPPOBAddressChoice(profile.registrationID, profile.companyTaxRegistration.transactionId, success.chosenAddress) flatMap {
              case DownstreamOutcome.Success  => Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails()))
              case DownstreamOutcome.Failure  => Future.successful(InternalServerError(views.html.pages.error.restart()))
              case DownstreamOutcome.Redirect => addressLookupService.buildAddressLookupUrl("ppob", controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress()) map {
                redirectUrl => Redirect(redirectUrl)
              }
            }
          )
        }
  }

  private def submitPPOBAddressChoice(regId: String, txId: String, choice: AddressChoice)(implicit user: AuthContext, hc: HeaderCarrier, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    choice match {
      case PPOBAddress =>
        Future.successful(DownstreamOutcome.Success)
      case ROAddress =>
        for {
          res <- companyDetailsService.copyROAddrToPPOBAddr(regId, txId)
          _   <- auditService.auditPPOBAddress(regId)
        } yield res
      case Other =>
        Future.successful(DownstreamOutcome.Redirect)
      case prepop: PrepopAddress => (for {
        prepopAddress <- prepopService.getAddress(regId, prepop.index)
        res           <- companyDetailsService.submitPPOBAddr(prepopAddress, regId, txId)
      } yield res) recover {
        case e: S4LFetchException =>
          Logger.warn(s"[CompanyDetailsController] [submitPPOBAddressChoice] - Error while saving PPOB Address with a PrepopAddress: ${e.getMessage}")
          DownstreamOutcome.Failure
      }
      case CorrespondenceAddress =>
        Logger.warn("[CompanyDetailsController] [submitPPOBAddressChoice] - Correspondence address returned as selected address in PPOB Address page")
        Future.successful(DownstreamOutcome.Failure)
    }
  }

  val savePPOBAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withCurrentProfile { profile =>
          for {
            Some(address) <- addressLookupService.getAddress
            res           <- companyDetailsService.submitPPOBAddr(address, profile.registrationID, profile.companyTaxRegistration.transactionId)
            _             <- prepopService.saveAddress(profile.registrationID, address)
          } yield res match {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        }
  }
}
