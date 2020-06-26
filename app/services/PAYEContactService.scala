/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors.PAYERegistrationConnector
import enums.{CacheKeys, DownstreamOutcome}
import javax.inject.Inject
import models.Address
import models.api.{PAYEContact => PAYEContactAPI}
import models.external.AuditingInformation
import models.view.{PAYEContactDetails, CompanyDetails => CompanyDetailsView, PAYEContact => PAYEContactView}
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class PAYEContactServiceImpl @Inject()(val payeRegConnector: PAYERegistrationConnector,
                                       val s4LService: S4LService,
                                       val companyDetailsService: CompanyDetailsService,
                                       val prepopService: PrepopulationService,
                                       val auditService: AuditService) extends PAYEContactService

trait PAYEContactService {
  val payeRegConnector: PAYERegistrationConnector
  val s4LService: S4LService
  val companyDetailsService: CompanyDetailsService
  val prepopService: PrepopulationService
  val auditService: AuditService

  private[services] def viewToAPI(viewData: PAYEContactView): Either[PAYEContactView, PAYEContactAPI] = viewData match {
    case PAYEContactView(Some(contactDetails), Some(correspondenceAddress)) =>
      Right(PAYEContactAPI(contactDetails, correspondenceAddress))
    case _ => Left(viewData)
  }

  private[services] def apiToView(apiData: PAYEContactAPI): PAYEContactView = {
    PAYEContactView(Some(apiData.contactDetails), Some(apiData.correspondenceAddress))
  }

  private def saveToS4L(regId: String, viewData: PAYEContactView)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.saveForm[PAYEContactView](CacheKeys.PAYEContact.toString, viewData, regId).map(_ => viewData)
  }

  def getCorrespondenceAddresses(correspondenceAddress: Option[Address], companyDetails: CompanyDetailsView): Map[String, Address] = {
    correspondenceAddress map {
      case address@companyDetails.roAddress if companyDetails.ppobAddress.contains(companyDetails.roAddress) => Map("correspondence" -> address)
      case address@companyDetails.roAddress => Map("correspondence" -> address) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      case addr: Address if companyDetails.ppobAddress.contains(addr) => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr)
      case addr: Address => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
    } getOrElse {
      if (companyDetails.ppobAddress.contains(companyDetails.roAddress)) {
        Map("ro" -> companyDetails.roAddress)
      } else {
        Map("ro" -> companyDetails.roAddress) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      }
    }
  }

  def getPAYEContact(regId: String)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.fetchAndGet[PAYEContactView](CacheKeys.PAYEContact.toString, regId) flatMap {
      case Some(contactDetails) => Future.successful(contactDetails)
      case None => getPAYEContactView(regId)
    }
  }

  private[services] def getPAYEContactView(regId: String)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    for {
      view <- payeRegConnector.getPAYEContact(regId) flatMap {
        case Some(payeRegContactDetails) => Future.successful(apiToView(payeRegContactDetails))
        case None => prepopService.getPAYEContactDetails(regId) flatMap {
          case Some(prepopContactDetails) => Future.successful(PAYEContactView(Some(prepopContactDetails), None))
          case None => Future.successful(PAYEContactView(None, None))
        }
      }
      _ <- s4LService.saveForm[PAYEContactView](CacheKeys.PAYEContact.toString, view, regId)
    } yield view
  }

  def submitPAYEContact(viewModel: PAYEContactView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel).fold(
      incompleteView =>
        saveToS4L(regId, incompleteView) map { _ => DownstreamOutcome.Success },
      completeAPI =>
        for {
          details <- payeRegConnector.upsertPAYEContact(regId, completeAPI)
          clearData <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }

  def submitPayeContactDetails(regId: String, newViewData: PAYEContactDetails)
                              (implicit hc: HeaderCarrier, auditInfo: AuditingInformation, req: Request[AnyContent]): Future[DownstreamOutcome.Value] = {
    for {
      cachedContactData <- getPAYEContact(regId) flatMap {
        case currentView if dataHasChanged(newViewData, currentView.contactDetails) =>
          for {
            _ <- auditService.auditPAYEContactDetails(regId, newViewData, currentView.contactDetails)
            _ <- prepopService.saveContactDetails(regId, newViewData) map {
              _ => Logger.info(s"[PAYEContactService] [submitPayeContactDetails] Successfully saved Contact Details to Prepopulation for regId: $regId")
            }
          } yield currentView
        case currentView =>
          Future.successful(currentView)
      }
      submitted <- submitPAYEContact(PAYEContactView(Some(newViewData), cachedContactData.correspondenceAddress), regId)
    } yield submitted
  }

  def dataHasChanged(viewData: PAYEContactDetails, s4lData: Option[PAYEContactDetails]): Boolean = s4lData.isEmpty || s4lData.exists(flattenData(viewData) != flattenData(_))

  def submitCorrespondence(regId: String, correspondenceAddress: Address)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact(regId) flatMap {
      data => submitPAYEContact(PAYEContactView(data.contactDetails, Some(correspondenceAddress)), regId)
    }
  }

  private[services] def flattenData(data: PAYEContactDetails) = data.copy(name = data.name.trim.replace(" ", "").toLowerCase, digitalContactDetails = data.digitalContactDetails.copy(
    email = data.digitalContactDetails.email map (_.trim.replace(" ", "").toLowerCase),
    phoneNumber = data.digitalContactDetails.phoneNumber map (_.trim.replace(" ", "").toLowerCase),
    mobileNumber = data.digitalContactDetails.mobileNumber map (_.trim.replace(" ", "").toLowerCase))
  )
}
