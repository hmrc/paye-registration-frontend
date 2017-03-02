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

import auth.PAYERegime
import javax.inject.{Inject, Singleton}

import config.FrontendAuthConnector
import connectors.{KeystoreConnect, KeystoreConnector}
import enums.DownstreamOutcome
import forms.companyDetails.{BusinessContactDetailsForm, ChooseAddressForm, TradingNameForm}
import models.view.{AddressChoice, ChosenAddress, TradingName}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.pages.companyDetails.{confirmROAddress, businessContactDetails => BusinessContactDetailsPage, ppobAddress => PPOBAddressPage, tradingName => TradingNamePage}

import scala.concurrent.Future

@Singleton
class CompanyDetailsController @Inject()(
                                          injS4LService: S4LService,
                                          injKeystoreConnector: KeystoreConnector,
                                          injCompanyDetailsService: CompanyDetailsService,
                                          injCohoService: CoHoAPIService,
                                          injMessagesApi: MessagesApi,
                                          injAddressLookupService: AddressLookupService)
  extends CompanyDetailsCtrl {
  val authConnector = FrontendAuthConnector
  val s4LService = injS4LService
  val keystoreConnector = injKeystoreConnector
  val companyDetailsService = injCompanyDetailsService
  val cohoService = injCohoService
  val messagesApi = injMessagesApi
  val addressLookupService = injAddressLookupService
}

trait CompanyDetailsCtrl extends FrontendController with Actions with I18nSupport {
  val s4LService: S4LSrv
  val keystoreConnector: KeystoreConnect
  val companyDetailsService: CompanyDetailsSrv
  val cohoService: CoHoAPISrv
  val addressLookupService: AddressLookupSrv

  val tradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>

    for {
      companyDetails <- companyDetailsService.getCompanyDetails
    } yield companyDetails.tradingName match {
      case Some(model) => Ok(TradingNamePage(TradingNameForm.form.fill(model), companyDetails.companyName))
      case _ => Ok(TradingNamePage(TradingNameForm.form, companyDetails.companyName))
    }
  }

  val submitTradingName = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async { implicit user => implicit request =>

    TradingNameForm.form.bindFromRequest.fold(
      errors => badRequestResponse(errors),
      success => {
        val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
        if (validatedForm.hasErrors) {
          badRequestResponse(validatedForm)
        } else {
          companyDetailsService.submitTradingName(success) map {
            case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.roAddress())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        }
      }
    )
  }

  val roAddress : Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          companyDetails <- companyDetailsService.getCompanyDetails
        } yield Ok(confirmROAddress(companyDetails.companyName, companyDetails.roAddress))
  }

  val confirmRO : Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.ppobAddress()))
  }

  private def badRequestResponse(form: Form[TradingName])(implicit request: Request[AnyContent]): Future[Result] = {
    cohoService.getStoredCompanyName map {
      cName => BadRequest(TradingNamePage(form, cName))
    }
  }

  val businessContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        companyDetailsService.getCompanyDetails map {
          details => details.businessContactDetails match {
            case Some(bcd) => Ok(BusinessContactDetailsPage(details.companyName, BusinessContactDetailsForm.form.fill(bcd)))
            case _ => Ok(BusinessContactDetailsPage(details.companyName, BusinessContactDetailsForm.form))
          }
        }
  }

  val submitBusinessContactDetails = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        BusinessContactDetailsForm.form.bindFromRequest.fold(
          errs => companyDetailsService.getCompanyDetails map (details => BadRequest(BusinessContactDetailsPage(details.companyName, errs))),
          success => companyDetailsService.submitBusinessContact(success) map {
            case DownstreamOutcome.Success => Redirect(routes.NatureOfBusinessController.natureOfBusiness())
            case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
          }
        )
  }

  val ppobAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        companyDetailsService.getCompanyDetails.map { companyDetails =>
          val addressMap = companyDetailsService.getPPOBPageAddresses(companyDetails)
          Ok(PPOBAddressPage(ChooseAddressForm.form.fill(ChosenAddress(AddressChoice.ppobAddress)), addressMap.get("ro"), addressMap.get("ppob")))
        }
  }

  val submitPPOBAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        ChooseAddressForm.form.bindFromRequest.fold(
          errs => companyDetailsService.getCompanyDetails map{
            details =>
              val addressMap = companyDetailsService.getPPOBPageAddresses(details)
              BadRequest(PPOBAddressPage(errs, addressMap.get("ro"), addressMap.get("ppob")))
          },
          success => success.chosenAddress match {
            case AddressChoice.ppobAddress =>
              Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails()))
            case AddressChoice.roAddress =>
              companyDetailsService.copyROAddrToPPOBAddr() map(_ => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails()))
            case AddressChoice.other => Future.successful(Redirect(addressLookupService.buildAddressLookupUrl("payereg1", controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress())))
          }
        )
  }

  val savePPOBAddress: Action[AnyContent] = AuthorisedFor(taxRegime = new PAYERegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          Some(address) <- addressLookupService.getAddress
          res <- companyDetailsService.submitPPOBAddr(address)
        } yield res match {
          case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails())
          case DownstreamOutcome.Failure => InternalServerError(views.html.pages.error.restart())
        }
  }
}
