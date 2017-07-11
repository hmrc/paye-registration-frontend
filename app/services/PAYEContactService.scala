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

package services

import javax.inject.{Inject, Singleton}

import connectors._
import enums.CacheKeys
import models.Address
import models.view.{CompanyDetails => CompanyDetailsView, PAYEContact => PAYEContactView}
import models.api.{PAYEContact => PAYEContactAPI}
import enums.DownstreamOutcome
import models.view.PAYEContactDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYEContactService @Inject()(injPAYERegistrationConnector: PAYERegistrationConnector,
                                   injS4LService: S4LService) extends PAYEContactSrv {
  override val payeRegConnector = injPAYERegistrationConnector
  override val s4LService = injS4LService
}

trait PAYEContactSrv  {
  val payeRegConnector: PAYERegistrationConnect
  val s4LService: S4LSrv


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

  private[services] def convertOrCreatePAYEContactView(oAPI: Option[PAYEContactAPI])(implicit hc: HeaderCarrier): PAYEContactView = {
    oAPI match {
      case Some(detailsAPI) => apiToView(detailsAPI)
      case None => PAYEContactView(None, None)
    }
  }

  def getCorrespondenceAddresses(correspondenceAddress: Option[Address], companyDetails: CompanyDetailsView): Map[String, Address] = {
    correspondenceAddress map {
      case address@companyDetails.roAddress if companyDetails.ppobAddress.contains(companyDetails.roAddress) => Map("correspondence" -> address)
      case address@companyDetails.roAddress => Map("correspondence" -> address) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      case addr: Address if companyDetails.ppobAddress.contains(addr) => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr)
      case addr: Address => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
    } getOrElse {
      if( companyDetails.ppobAddress.contains(companyDetails.roAddress) ) {
        Map("ro" -> companyDetails.roAddress)
      } else {
        Map("ro" -> companyDetails.roAddress) ++ companyDetails.ppobAddress.map(("ppob", _)).toMap
      }
    }
  }

  def getPAYEContact(regId: String)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.fetchAndGet[PAYEContactView](CacheKeys.PAYEContact.toString, regId) flatMap {
      case Some(contactDetails) => Future.successful(contactDetails)
      case None => for {
        oDetails <- payeRegConnector.getPAYEContact(regId)
      } yield convertOrCreatePAYEContactView(oDetails)
    }
  }

  def submitPAYEContact(viewModel: PAYEContactView, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel).fold(
      incompleteView =>
        saveToS4L(regId, incompleteView) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          details   <- payeRegConnector.upsertPAYEContact(regId, completeAPI)
          clearData <- s4LService.clear(regId)
        } yield DownstreamOutcome.Success
    )
  }

  def submitPayeContactDetails(viewData: PAYEContactDetails, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact(regId) flatMap {
      data => submitPAYEContact(PAYEContactView(Some(viewData), data.correspondenceAddress), regId)
    }
  }

  def submitCorrespondence(correspondenceAddress: Address, regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact(regId) flatMap {
      data => submitPAYEContact(PAYEContactView(data.contactDetails, Some(correspondenceAddress)), regId)
    }
  }
}

