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

import connectors.{BusinessRegistrationConnect, BusinessRegistrationConnector}
import models.DigitalContactDetails
import models.view.PAYEContactDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PrepopulationService @Inject()(injBusinessRegistrationConnector: BusinessRegistrationConnector) extends PrepopulationSrv {
  override val busRegConnector = injBusinessRegistrationConnector
}

trait PrepopulationSrv {

  val busRegConnector: BusinessRegistrationConnect

  def getBusinessContactDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[DigitalContactDetails]] = {
    busRegConnector.retrieveContactDetails(regId) map {
      case Some(contactDetails) => Some(DigitalContactDetails(contactDetails.digitalContactDetails.email,
                                                              contactDetails.digitalContactDetails.mobileNumber,
                                                              contactDetails.digitalContactDetails.phoneNumber))
      case None => None
    }
  }

  def saveContactDetails(regId: String, contactDetails: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[PAYEContactDetails] = {
    busRegConnector.upsertContactDetails(regId, contactDetails) map {
      _ => contactDetails
    }
  }
}
