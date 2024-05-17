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

package controllers.userJourney

import common.exceptions.S4LFetchExceptionType
import config.AppConfig
import connectors.{IncorporationInformationConnector, KeystoreConnector}
import controllers.{AuthRedirectUrls, PayeBaseController}
import enums.DownstreamOutcome
import forms.companyDetails.{BusinessContactDetailsForm, PPOBForm, TradingNameForm}
import models.external.AuditingInformation
import models.view._
import play.api.data.Form
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.companyDetails.{confirmROAddress, businessContactDetails => BusinessContactDetailsPage, ppobAddress => PPOBAddressPage, tradingName => TradingNamePage}
import views.html.pages.error.restart

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompanyDetailsController @Inject()(val s4LService: S4LService,
                                         val keystoreConnector: KeystoreConnector,
                                         val companyDetailsService: CompanyDetailsService,
                                         val incorpInfoService: IncorporationInformationService,
                                         val authConnector: AuthConnector,
                                         val addressLookupService: AddressLookupService,
                                         val prepopService: PrepopulationService,
                                         val auditService: AuditService,
                                         val incorporationInformationConnector: IncorporationInformationConnector,
                                         val payeRegistrationService: PAYERegistrationService,
                                         mcc: MessagesControllerComponents,
                                         TradingNamePage: TradingNamePage,
                                         restart: restart,
                                         BusinessContactDetailsPage: BusinessContactDetailsPage,
                                         confirmROAddress: confirmROAddress,
                                         PPOBAddressPage: PPOBAddressPage
                                        )(implicit val appConfig: AppConfig,
                                          implicit val ec: ExecutionContext) extends PayeBaseController(mcc) with AuthRedirectUrls {

  val companyNameKey: String = "CompanyName"

  def tradingName: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      for {
        companyDetails <- companyDetailsService.withLatestCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
        tName <- companyDetailsService.getTradingNamePrepop(profile.registrationID, companyDetails.tradingName)
      } yield {
        Ok(TradingNamePage(TradingNameForm.fillWithPrePop(tName, companyDetails.tradingName), companyDetails.companyName))
      }
  }

  def submitTradingName: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      TradingNameForm.form.bindFromRequest().fold(
        errors => badRequestResponse(profile.registrationID, profile.companyTaxRegistration.transactionId, errors),
        success => {
          val validatedForm = TradingNameForm.validateForm(TradingNameForm.form.fill(success))
          if (validatedForm.hasErrors) {
            badRequestResponse(profile.registrationID, profile.companyTaxRegistration.transactionId, validatedForm)
          } else {
            val trimmedTradingName = success.copy(tradingName = success.tradingName.map(_.trim))
            companyDetailsService.submitTradingName(trimmedTradingName, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
              case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.roAddress)
              case DownstreamOutcome.Failure => InternalServerError(restart())
            }
          }
        }
      )
  }

  def roAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      companyDetailsService.withLatestCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId).map { companyDetails =>
        Ok(confirmROAddress(companyDetails.companyName, companyDetails.roAddress))
      }
  }

  def confirmRO: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    _ =>
      Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.ppobAddress))
  }

  private def badRequestResponse(regId: String, txID: String, form: Form[TradingName])(implicit request: Request[AnyContent]): Future[Result] = {
    companyDetailsService.getCompanyDetails(regId, txID) map {
      details => BadRequest(TradingNamePage(form, details.companyName))
    }
  }

  def businessContactDetails: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId) flatMap {
        details =>
          details.businessContactDetails match {
            case Some(bcd) => Future.successful(Ok(BusinessContactDetailsPage(BusinessContactDetailsForm.form.fill(bcd), details.companyName))
              .addingToSession(companyNameKey -> details.companyName))
            case None => Future.successful(Ok(BusinessContactDetailsPage(BusinessContactDetailsForm.form, details.companyName))
              .addingToSession(companyNameKey -> details.companyName))
          }
      }
  }

  def submitBusinessContactDetails: Action[AnyContent] = isAuthorisedWithProfileAndAuditing { implicit request =>
    val optCompanyName = request.session.get(companyNameKey).filter(_.nonEmpty)
    profile =>
      implicit audit =>
        optCompanyName match {
          case Some(companyName) => BusinessContactDetailsForm.form.bindFromRequest().fold(
            errs => Future.successful(BadRequest(BusinessContactDetailsPage(errs, companyName))),
            success => {
              val trimmed = success.copy(email = success.email map (_.trim), phoneNumber = success.phoneNumber map (_.trim), mobileNumber = success.mobileNumber map (_.trim))
              companyDetailsService.submitBusinessContact(trimmed, profile.registrationID, profile.companyTaxRegistration.transactionId) map {
                case DownstreamOutcome.Success => Redirect(routes.NatureOfBusinessController.natureOfBusiness)
                  .removingFromSession(companyNameKey)
                case DownstreamOutcome.Failure => InternalServerError(restart())
                  .removingFromSession(companyNameKey)
              }
            }
          )
          case None => Future.successful(Redirect(routes.CompanyDetailsController.businessContactDetails).removingFromSession(companyNameKey))
        }
  }

  def ppobAddress: Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      for {
        companyDetails <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
        prepopAddress <- prepopService.getPrePopAddresses(profile.registrationID, companyDetails.roAddress, companyDetails.ppobAddress, None)
          .map(_.headOption.map(_.toString))
      } yield {
        val addressMap = companyDetailsService.getPPOBPageAddresses(companyDetails)
          .collect { case (id, address) => id -> address.toString }
        Ok(
          PPOBAddressPage(
            PPOBForm.form.fill(ChosenAddress(PPOBAddress)),
            addressMap,
            prepopAddress))
      }
  }

  def submitPPOBAddress: Action[AnyContent] = isAuthorisedWithProfileAndAuditing { implicit request =>
    profile =>
      implicit audit =>
        PPOBForm.form.bindFromRequest().fold(
          errs => for {
            details <- companyDetailsService.getCompanyDetails(profile.registrationID, profile.companyTaxRegistration.transactionId)
            prepopAddress <- prepopService.getPrePopAddresses(profile.registrationID, details.roAddress, details.ppobAddress, None)
              .map(_.headOption.map(_._2.toString))
          } yield {
            val addressMap = companyDetailsService.getPPOBPageAddresses(details)
              .collect { case (id, address) => id -> address.toString }
            BadRequest(PPOBAddressPage(errs,
              addressMap,
              prepopAddress))
          },
          success => submitPPOBAddressChoice(profile.registrationID, profile.companyTaxRegistration.transactionId, success.chosenAddress) flatMap {
            case DownstreamOutcome.Success => Future.successful(Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails))
            case DownstreamOutcome.Failure => Future.successful(InternalServerError(restart()))
            case DownstreamOutcome.Redirect => addressLookupService.buildAddressLookupUrl("ppob", controllers.userJourney.routes.CompanyDetailsController.savePPOBAddress(None)) map {
              redirectUrl => Redirect(redirectUrl)
            }
          }
        )
  }

  private def submitPPOBAddressChoice(regId: String, txId: String, choice: AddressChoice)
                                     (implicit auditInfo: AuditingInformation, hc: HeaderCarrier, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    choice match {
      case PPOBAddress =>
        Future.successful(DownstreamOutcome.Success)
      case ROAddress =>
        for {
          res <- companyDetailsService.copyROAddrToPPOBAddr(regId, txId)
          _ <- auditService.auditPPOBAddress(regId)
        } yield res
      case Other =>
        Future.successful(DownstreamOutcome.Redirect)
      case prepop: PrepopAddress => (for {
        prepopAddress <- prepopService.getAddress(regId, prepop.index)
        res <- companyDetailsService.submitPPOBAddr(prepopAddress, regId, txId)
      } yield res) recover {
        case e: S4LFetchExceptionType =>
          warnLog(s"[submitPPOBAddressChoice] Error while saving PPOB Address with a PrepopAddress: ${e.getMessage}")
          DownstreamOutcome.Failure
      }
      case CorrespondenceAddress =>
        warnLog("[submitPPOBAddressChoice] Correspondence address returned as selected address in PPOB Address page")
        Future.successful(DownstreamOutcome.Failure)
    }
  }

  def savePPOBAddress(alfId: Option[String]): Action[AnyContent] = isAuthorisedWithProfile { implicit request =>
    profile =>
      alfId match {
        case Some(id) => for {
          address <- addressLookupService.getAddress(id)
          res <- companyDetailsService.submitPPOBAddr(address, profile.registrationID, profile.companyTaxRegistration.transactionId)
          _ <- prepopService.saveAddress(profile.registrationID, address)
        } yield res match {
          case DownstreamOutcome.Success => Redirect(controllers.userJourney.routes.CompanyDetailsController.businessContactDetails)
          case DownstreamOutcome.Failure => InternalServerError(restart())
        }
        case None =>
          throw new Exception("[savePPOBAddress] 'id' query string missing from ALF handback")
      }
  }
}
