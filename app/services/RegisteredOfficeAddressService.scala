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
import models.external.CHROAddress
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RegisteredOfficeAddressService @Inject()(injCompRegConnector : CompanyRegistrationConnector,
                                               injCohoAPIConnector: CoHoAPIConnector) extends RegisteredOfficeAddressSrv {
  val compRegConnector = injCompRegConnector
  val cohoAPIConnector = injCohoAPIConnector
}

trait RegisteredOfficeAddressSrv {

  val compRegConnector: CompanyRegistrationConnect
  val cohoAPIConnector: CoHoAPIConnect

  def retrieveRegisteredOfficeAddress(registrationId: String)(implicit hc : HeaderCarrier): Future[CHROAddress] = {
    for {
      tID <- compRegConnector.getTransactionId(registrationId)
      address <- cohoAPIConnector.getRegisteredOfficeAddress(tID)
    } yield {
      address
    }
  }
}
