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

import connectors.{CoHoAPIConnect, CoHoAPIConnector, CompanyRegistrationConnect, CompanyRegistrationConnector, KeystoreConnector, PAYERegistrationConnect, PAYERegistrationConnector}
import enums.{CacheKeys, DownstreamOutcome}
import models.PAYEContactDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PAYEContactService @Inject()(injKeystoreConnector: KeystoreConnector,
                                   injPAYERegistrationConnector: PAYERegistrationConnector,
                                   injCoHoAPIService: CoHoAPIService,
                                   injS4LService: S4LService,
                                   injCompRegConnector : CompanyRegistrationConnector,
                                   injCohoAPIConnector: CoHoAPIConnector) extends PAYEContactSrv {

  override val keystoreConnector = injKeystoreConnector
  override val payeRegConnector = injPAYERegistrationConnector
  override val compRegConnector = injCompRegConnector
  override val cohoAPIConnector = injCohoAPIConnector
  override val cohoService = injCoHoAPIService
  override val s4LService = injS4LService
}

trait PAYEContactSrv extends CommonService {
  val payeRegConnector: PAYERegistrationConnect
  val compRegConnector: CompanyRegistrationConnect
  val cohoAPIConnector: CoHoAPIConnect
  val cohoService: CoHoAPISrv
  val s4LService: S4LSrv

  def getPAYEContact(implicit hc: HeaderCarrier): Future[Option[PAYEContactDetails]] = {
    for {
      regID    <- fetchRegistrationID
      oPayeContact <- payeRegConnector.getPAYEContact(regID)
    } yield oPayeContact
  }

  def submitPAYEContact(payeContact: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      regID     <- fetchRegistrationID
      details   <- payeRegConnector.upsertPAYEContact(regID, payeContact)
    } yield DownstreamOutcome.Success
  }
}
