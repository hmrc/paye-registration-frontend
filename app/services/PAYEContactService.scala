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
import enums.{CacheKeys, DownstreamOutcome}
import models.Address
import models.view.{CompanyDetails => CompanyDetailsView, PAYEContactDetails, PAYEContact => PAYEContactView}
import models.api.{PAYEContact => PAYEContactAPI}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PAYEContactService @Inject()(injKeystoreConnector: KeystoreConnector,
                                   injPAYERegistrationConnector: PAYERegistrationConnector,
                                   injS4LService: S4LService) extends PAYEContactSrv {

  override val keystoreConnector = injKeystoreConnector
  override val payeRegConnector = injPAYERegistrationConnector
  override val s4LService = injS4LService
}

trait PAYEContactSrv extends CommonService {
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

  private def saveToS4L(viewData: PAYEContactView)(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.saveForm[PAYEContactView](CacheKeys.PAYEContact.toString, viewData).map(_ => viewData)
  }

  private[services] def convertOrCreatePAYEContactView(oAPI: Option[PAYEContactAPI])(implicit hc: HeaderCarrier): PAYEContactView = {
    oAPI match {
      case Some(detailsAPI) => apiToView(detailsAPI)
      case None => PAYEContactView(None, None)
    }
  }

  def getCorrespondenceAddresses(correspondenceAddress: Option[Address], companyDetails: CompanyDetailsView): Map[String, Address] = {
    correspondenceAddress map {
      case address@companyDetails.roAddress => Map("correspondence" -> address)
      case addr: Address => Map("ro" -> companyDetails.roAddress, "correspondence" -> addr)
    } getOrElse Map("ro" -> companyDetails.roAddress)
  }

  def getPAYEContact(implicit hc: HeaderCarrier): Future[PAYEContactView] = {
    s4LService.fetchAndGet[PAYEContactView](CacheKeys.PAYEContact.toString) flatMap {
      case Some(contactDetails) => Future.successful(contactDetails)
      case None => for {
        regId <- fetchRegistrationID
        oDetails <- payeRegConnector.getPAYEContact(regId)
      } yield convertOrCreatePAYEContactView(oDetails)
    }
  }

  def submitPAYEContact(viewModel: PAYEContactView)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    viewToAPI(viewModel).fold(
      incompleteView =>
        saveToS4L(incompleteView) map {_ => DownstreamOutcome.Success},
      completeAPI =>
        for {
          regID     <- fetchRegistrationID
          details   <- payeRegConnector.upsertPAYEContact(regID, completeAPI)
          clearData <- s4LService.clear
        } yield DownstreamOutcome.Success
    )
  }

  def submitPayeContactDetails(viewData: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact flatMap {
      data => submitPAYEContact(PAYEContactView(Some(viewData), data.correspondenceAddress))
    }
  }

  def submitCorrespondence(correspondenceAddress: Address)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    getPAYEContact flatMap {
      data => submitPAYEContact(PAYEContactView(data.contactDetails, Some(correspondenceAddress)))
    }
  }
}

