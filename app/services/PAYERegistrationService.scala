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

import config.FrontendAuthConnector
import enums.AccountTypes
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import connectors._
import enums.DownstreamOutcome
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PAYERegistrationService @Inject()(payeRegistrationConn: PAYERegistrationConnector) extends PAYERegistrationSrv {
  override val payeRegistrationConnector = payeRegistrationConn
  override val authConnector = FrontendAuthConnector
}

trait PAYERegistrationSrv {

  val payeRegistrationConnector: PAYERegistrationConnect
  val authConnector: AuthConnector

  def assertRegistrationFootprint(regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    payeRegistrationConnector.createNewRegistration(regId)
  }

  def getAccountAffinityGroup(implicit hc: HeaderCarrier, authContext: AuthContext): Future[AccountTypes.Value] = {
    authConnector.getUserDetails[JsObject](authContext) flatMap { userDetails =>
      (userDetails \ "affinityGroup").as[String] match {
        case "Organisation" => Future.successful(AccountTypes.Organisation)
        case _              => Future.successful(AccountTypes.InvalidAccountType)
      }
    }
  }
}
