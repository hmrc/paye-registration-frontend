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
import models.Address
import models.external.BusinessProfile
import models.view.{CompanyDetails, PAYEContactDetails}
import play.api.libs.json.{JsValue, Reads}
import services.MetricsService
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegistrationConnector @Inject()(val metricsService: MetricsService,
                                                  val http: HttpClient,
                                                  appConfig: AppConfig)(implicit val ec: ExecutionContext) {

  val businessRegUrl = appConfig.servicesConfig.baseUrl("business-registration")

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[BusinessProfile]): Future[BusinessProfile] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration") map { profile =>
      businessRegistrationTimer.stop()
      profile
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        throw logResponse(e, "retrieveCurrentProfile", "retrieving current profile")
    }
  }

  def retrieveCompletionCapacity(implicit hc: HeaderCarrier, rds: HttpReads[JsValue]): Future[Option[String]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[JsValue](s"$businessRegUrl/business-registration/business-tax-registration") map { json =>
      businessRegistrationTimer.stop()
      (json \ "completionCapacity").asOpt[String]
    } recover {
      case _: NotFoundException =>
        businessRegistrationTimer.stop()
        None
      case e =>
        businessRegistrationTimer.stop()
        throw logResponse(e, "retrieveCompletionCapacity", "retrieving completion capacity")
    }
  }

  def retrieveTradingName(regId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[JsValue](s"$businessRegUrl/business-registration/$regId/trading-name") map {
      businessRegistrationTimer.stop()
      _.as[Option[String]](CompanyDetails.tradingNameApiPrePopReads)
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        logResponse(e, "retrieveTradingName", "retrieving Trading Name from pre-pop", Some(regId))
        None
    }
  }

  def upsertTradingName(regId: String, tradingName: String)(implicit hc: HeaderCarrier): Future[String] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val prepopWrites = CompanyDetails.tradingNameApiPrePopWrites
    http.POST[String, HttpResponse](s"$businessRegUrl/business-registration/$regId/trading-name", tradingName) map {
      _ =>
        businessRegistrationTimer.stop()
        tradingName
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        logResponse(e, "upsertTradingName", "upserting Trading Name to pre-pop", Some(regId))
        tradingName
    }
  }

  def retrieveContactDetails(regId: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContactDetails]): Future[Option[PAYEContactDetails]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val rds: Reads[PAYEContactDetails] = PAYEContactDetails.prepopReads
    http.GET[PAYEContactDetails](s"$businessRegUrl/business-registration/$regId/contact-details") map { details =>
      businessRegistrationTimer.stop()
      Some(details)
    } recover {
      case _: NotFoundException =>
        businessRegistrationTimer.stop()
        None
      case e =>
        businessRegistrationTimer.stop()
        logResponse(e, "retrieveContactDetails", "retrieving contact details")
        None
    }
  }

  def upsertContactDetails(regId: String, contactDetails: PAYEContactDetails)(implicit hc: HeaderCarrier): Future[PAYEContactDetails] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val wts = PAYEContactDetails.prepopWrites
    http.POST[PAYEContactDetails, HttpResponse](s"$businessRegUrl/business-registration/$regId/contact-details", contactDetails) map { _ =>
      businessRegistrationTimer.stop()
      contactDetails
    } recover {
      case e: Exception =>
        businessRegistrationTimer.stop()
        logResponse(e, "upsertContactDetails", "upserting contact details")
        contactDetails
    }
  }

  def retrieveAddresses(regId: String)(implicit hc: HeaderCarrier): Future[Seq[Address]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[JsValue](s"$businessRegUrl/business-registration/$regId/addresses") map { json =>
      businessRegistrationTimer.stop()
      json.\("addresses").as[Seq[JsValue]].map(_.as[Address](Address.prePopReads))
    } recover {
      case _: NotFoundException =>
        businessRegistrationTimer.stop()
        Seq.empty
      case ex =>
        businessRegistrationTimer.stop()
        logResponse(ex, "retrieveAddresses", "fetching addresses from pre-pop", Some(regId))
        Seq.empty
    }
  }

  def upsertAddress(regId: String, address: Address)(implicit hc: HeaderCarrier): Future[Address] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val wts = Address.prePopWrites
    http.POST[Address, HttpResponse](s"$businessRegUrl/business-registration/$regId/addresses", address) map { _ =>
      businessRegistrationTimer.stop()
      address
    } recover {
      case e: Exception =>
        businessRegistrationTimer.stop()
        logResponse(e, "upsertAddress", "upserting address")
        address
    }
  }
}
