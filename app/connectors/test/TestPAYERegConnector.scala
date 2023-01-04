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

package connectors.test

import config.AppConfig
import connectors._
import connectors.httpParsers.{BaseHttpReads, PAYERegistrationHttpParsers}
import enums.DownstreamOutcome
import models.api.{Employment, CompanyDetails => CompanyDetailsAPI, PAYEContact => PAYEContactAPI, PAYERegistration => PAYERegistrationAPI}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestPAYERegConnectorImpl @Inject()(val payeRegConnector: PAYERegistrationConnector,
                                         val http: HttpClient,
                                         appConfig: AppConfig, implicit val ec: ExecutionContext) extends TestPAYERegConnector {
  val payeRegUrl = appConfig.servicesConfig.baseUrl("paye-registration")
}

trait TestPAYERegConnector extends BaseConnector with PAYERegistrationHttpParsers {

  implicit val ec: ExecutionContext
  val payeRegUrl: String
  val http: CoreGet with CorePost
  val payeRegConnector: PAYERegistrationConnector

  def addPAYERegistration(reg: PAYERegistrationAPI)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.POST[PAYERegistrationAPI, DownstreamOutcome.Value](s"$payeRegUrl/paye-registration/test-only/update-registration/${reg.registrationID}", reg)(
      PAYERegistrationAPI.format, createNewRegistrationHttpReads(reg.registrationID, reg.transactionID), hc, ec
    )
  }

  def addTestCompanyDetails(details: CompanyDetailsAPI, regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] =
    payeRegConnector.upsertCompanyDetails(regId, details) map {
      _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }

  def addTestPAYEContact(details: PAYEContactAPI, regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] =
    payeRegConnector.upsertPAYEContact(regId, details) map {
      _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }

  def addTestEmploymentInfo(details: Employment, regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[DownstreamOutcome.Value] =
    payeRegConnector.upsertEmployment(regId, details) map {
      _ => DownstreamOutcome.Success
    } recover {
      case _ => DownstreamOutcome.Failure
    }

  def testRegistrationTeardown()(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.GET[HttpResponse](s"$payeRegUrl/paye-registration/test-only/registration-teardown")(rawReads, hc, ec) map {
      _ => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        logger.warn(s"[testRegistrationTeardown] received error when clearing registration details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

  def tearDownIndividualRegistration(regId: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.GET[HttpResponse](s"$payeRegUrl/paye-registration/test-only/delete-registration/$regId")(rawReads, hc, ec) map {
      _ => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        logger.warn(s"[tearDownIndividualRegistration] received error when clearing registration details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }

  def updateStatus(regId: String, status: String)(implicit hc: HeaderCarrier): Future[DownstreamOutcome.Value] = {
    http.POST[JsObject, HttpResponse](s"$payeRegUrl/paye-registration/test-only/update-status/$regId/$status", Json.obj())(implicitly, rawReads, hc, ec) map {
      _ => DownstreamOutcome.Success
    } recover {
      case e: Exception =>
        logger.warn(s"[updateStatus] received error when updating status details - Error: ${e.getMessage}")
        DownstreamOutcome.Failure
    }
  }
}
