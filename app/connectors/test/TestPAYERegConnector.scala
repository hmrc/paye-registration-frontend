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

package connectors.test

import javax.inject.{Inject, Singleton}

import config.WSHttp
import connectors._
import enums.DownstreamOutcome
import models.api.{CompanyDetails => CompanyDetailsAPI, PAYERegistration => PAYERegistrationAPI, PAYEContact => PAYEContactAPI}
import play.api.Logger
import play.api.http.Status
import services.CommonService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestPAYERegConnector @Inject()(keystore : KeystoreConnector,payeRegistrationConnector: PAYERegistrationConnector) extends TestPAYERegConnect with ServicesConfig {
  val payeRegUrl = baseUrl("paye-registration")
  val http : WSHttp = WSHttp
  val keystoreConnector: KeystoreConnect = keystore
  val payeRegConnector: PAYERegistrationConnect = payeRegistrationConnector
}

trait TestPAYERegConnect extends CommonService {

  val payeRegUrl: String
  val http: WSHttp
  val payeRegConnector: PAYERegistrationConnect

  def addPAYERegistration(reg: PAYERegistrationAPI)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    for {
      resp <- http.POST[PAYERegistrationAPI, HttpResponse](s"$payeRegUrl/paye-registration/test-only/update-registration/${reg.registrationID}", reg)
    } yield resp.status match {
      case Status.OK => DownstreamOutcome.Success
      case _  =>        DownstreamOutcome.Failure
    }
  }

  def addTestCompanyDetails(details: CompanyDetailsAPI)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    val response = for {
      regID <- fetchRegistrationID
      resp <- payeRegConnector.upsertCompanyDetails(regID,  details)
    } yield resp

    response map {
        _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }
  }

  def addTestPAYEContact(details: PAYEContactAPI)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    val response = for {
      regID <- fetchRegistrationID
      resp <- payeRegConnector.upsertPAYEContact(regID,  details)
    } yield resp

    response map {
      _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }
  }

  def testRegistrationTeardown()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.GET[HttpResponse](s"$payeRegUrl/paye-registration/test-only/registration-teardown") map {
      resp => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        Logger.warn(s"[PAYERegistrationConnector] [testRegistrationTeardown] received error when clearing registration details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }
}
