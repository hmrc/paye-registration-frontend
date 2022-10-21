/*
 * Copyright 2022 HM Revenue & Customs
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
import services.MetricsService
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegistrationConnector @Inject()(val metricsService: MetricsService,
                                              val http: HttpClient,
                                              appConfig: AppConfig)(implicit val ec: ExecutionContext) extends BusinessRegistrationHttpParsers {

  val businessRegUrl = appConfig.servicesConfig.baseUrl("business-registration")

  def retrieveCurrentProfile(implicit hc: HeaderCarrier): Future[BusinessProfile] =
    withTimer {
      http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration")(businessProfileHttpReads, hc, ec)
    }

  def retrieveCompletionCapacity(implicit hc: HeaderCarrier): Future[Option[String]] =
    withTimer {
      http.GET[Option[String]](s"$businessRegUrl/business-registration/business-tax-registration")(retrieveCompletionCapacityHttpReads, hc, ec)
    }

  def retrieveTradingName(regId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    withDefaultResponseRecovery[Option[String]](None)("retrieveTradingName", regId) {
      withTimer {
        http.GET[Option[String]](s"$businessRegUrl/business-registration/$regId/trading-name")(retrieveTradingNameHttpReads, hc, ec)
      }
    }

  def upsertTradingName(regId: String, tradingName: String)(implicit hc: HeaderCarrier): Future[String] =
    withDefaultResponseRecovery(tradingName)("upsertTradingName", regId) {
      withTimer {
        http.POST[String, String](s"$businessRegUrl/business-registration/$regId/trading-name", tradingName)(
          CompanyDetails.tradingNameApiPrePopWrites, upsertTradingNameHttpReads(tradingName), hc, ec
        )
      }
    }

  def retrieveContactDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[PAYEContactDetails]] =
    withDefaultResponseRecovery[Option[PAYEContactDetails]](None)("retrieveContactDetails", regId) {
      withTimer {
        http.GET[Option[PAYEContactDetails]](s"$businessRegUrl/business-registration/$regId/contact-details")(retrieveContactDetailsHttpReads, hc, ec)
      }
    }

  def upsertContactDetails(regId: String, contactDetails: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[PAYEContactDetails] =
    withDefaultResponseRecovery(contactDetails)("upsertContactDetails", regId) {
      withTimer {
        http.POST[PAYEContactDetails, PAYEContactDetails](s"$businessRegUrl/business-registration/$regId/contact-details", contactDetails)(
          PAYEContactDetails.prepopWrites, upsertContactDetailsHttpReads(contactDetails), hc, ec
        )
      }
    }

  def retrieveAddresses(regId: String)(implicit hc: HeaderCarrier): Future[Seq[Address]] =
    withDefaultResponseRecovery(Seq.empty[Address])("retrieveAddresses", regId) {
      withTimer {
        http.GET[Seq[Address]](s"$businessRegUrl/business-registration/$regId/addresses")(retrieveAddressesHttpReads, hc, ec)
      }
    }

  def upsertAddress(regId: String, address: Address)(implicit hc: HeaderCarrier): Future[Address] =
    withDefaultResponseRecovery(address)("upsertAddress", regId) {
      withTimer {
        http.POST[Address, Address](s"$businessRegUrl/business-registration/$regId/addresses", address)(
          Address.prePopWrites, upsertAddressHttpReads(address), hc, ec
        )
      }
    }

  private def withTimer[T](f: => Future[T]) =
    metricsService.processDataResponseWithMetrics(metricsService.businessRegistrationResponseTimer.time())(f)

  private def withDefaultResponseRecovery[T](response: => T)(functionName: String, regId: String)(f: => Future[T]) =
    f recover { case ex: Exception =>
      logger.error(s"[$functionName] Exception of type '${ex.getClass.getName}' was thrown for regId: $regId")
      response
    }

}
