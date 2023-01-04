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

package connectors

import config.AppConfig
import connectors.httpParsers.BusinessRegistrationHttpParsers
import models.Address
import models.external.BusinessProfile
import models.view.{CompanyDetails, PAYEContactDetails}
import play.api.mvc.Request
import services.MetricsService
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegistrationConnector @Inject()(val metricsService: MetricsService,
                                              val http: HttpClient,
                                              appConfig: AppConfig)(implicit val ec: ExecutionContext) extends BaseConnector with BusinessRegistrationHttpParsers {

  val businessRegUrl = appConfig.servicesConfig.baseUrl("business-registration")

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, request: Request[_]): Future[BusinessProfile] = {
    infoLog("[retrieveCurrentProfile] attempting to retrieveCurrentProfile")
    withTimer {
      withRecovery()("retrieveCurrentProfile") {
        http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration")(businessProfileHttpReads, hc, ec)
      }
    }
  }

  def retrieveCompletionCapacity(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] =
    withTimer {
      withRecovery()("retrieveCurrentProfile") {
        http.GET[Option[String]](s"$businessRegUrl/business-registration/business-tax-registration")(retrieveCompletionCapacityHttpReads, hc, ec)
      }
    }

  def retrieveTradingName(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] =
    withTimer {
      withRecovery(Some(Option.empty[String]))("retrieveTradingName", Some(regId)) {
        http.GET[Option[String]](s"$businessRegUrl/business-registration/$regId/trading-name")(retrieveTradingNameHttpReads(regId), hc, ec)
      }
    }

  def upsertTradingName(regId: String, tradingName: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[String] =
    withTimer {
      withRecovery(Some(tradingName))("upsertTradingName", Some(regId)) {
        http.POST[String, String](s"$businessRegUrl/business-registration/$regId/trading-name", tradingName)(
          CompanyDetails.tradingNameApiPrePopWrites, upsertTradingNameHttpReads(regId, tradingName), hc, ec
        )
      }
    }

  def retrieveContactDetails(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[PAYEContactDetails]] =
    withTimer {
      withRecovery(Some(Option.empty[PAYEContactDetails]))("retrieveContactDetails", Some(regId)) {
        http.GET[Option[PAYEContactDetails]](s"$businessRegUrl/business-registration/$regId/contact-details")(retrieveContactDetailsHttpReads(regId), hc, ec)
      }
    }

  def upsertContactDetails(regId: String, contactDetails: PAYEContactDetails)
                          (implicit hc: HeaderCarrier, request: Request[_]): Future[PAYEContactDetails] =
    withTimer {
      withRecovery(Some(contactDetails))("upsertContactDetails", Some(regId)) {
        http.POST[PAYEContactDetails, PAYEContactDetails](s"$businessRegUrl/business-registration/$regId/contact-details", contactDetails)(
          PAYEContactDetails.prepopWrites, upsertContactDetailsHttpReads(regId, contactDetails), hc, ec
        )
      }
    }

  def retrieveAddresses(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[Address]] =
    withRecovery(Some(Seq.empty[Address]))("retrieveAddresses", Some(regId)) {
      withTimer {
        http.GET[Seq[Address]](s"$businessRegUrl/business-registration/$regId/addresses")(retrieveAddressesHttpReads(regId), hc, ec)
      }
    }

  def upsertAddress(regId: String, address: Address)(implicit hc: HeaderCarrier, request: Request[_]): Future[Address] =
    withRecovery(Some(address))("upsertAddress", Some(regId)) {
      withTimer {
        http.POST[Address, Address](s"$businessRegUrl/business-registration/$regId/addresses", address)(
          Address.prePopWrites, upsertAddressHttpReads(regId, address), hc, ec
        )
      }
    }

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics(metricsService.businessRegistrationResponseTimer.time())(f)

}
