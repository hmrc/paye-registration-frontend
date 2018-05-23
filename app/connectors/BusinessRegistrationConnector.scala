/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import config.WSHttp
import models.Address
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import play.api.libs.json.JsValue
import services.MetricsService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class BusinessRegistrationConnectorImpl @Inject()(val metricsService: MetricsService,
                                                  val http: WSHttp,
                                                  servicesConfig: ServicesConfig) extends BusinessRegistrationConnector {
  val businessRegUrl = servicesConfig.baseUrl("business-registration")
}

trait BusinessRegistrationConnector {
  val businessRegUrl: String
  val http: CoreGet with CorePost
  val metricsService: MetricsService

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[BusinessProfile]): Future[BusinessProfile] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration") map { profile =>
      businessRegistrationTimer.stop()
      profile
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        throw logResponse(e, "retrieving current profile")
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
        throw logResponse(e, "retrieving completion capacity")
    }
  }

  def retrieveContactDetails(regId: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContactDetails]): Future[Option[PAYEContactDetails]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val rds = PAYEContactDetails.prepopReads
    http.GET[PAYEContactDetails](s"$businessRegUrl/business-registration/$regId/contact-details") map { details =>
      businessRegistrationTimer.stop()
      Some(details)
    } recover {
      case _: NotFoundException =>
        businessRegistrationTimer.stop()
        None
      case e =>
        businessRegistrationTimer.stop()
        logResponse(e, "retrieving contact details")
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
        logResponse(e, "upserting contact details")
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
        logResponse(ex, "fetching addresses from pre-pop", Some(regId))
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
        logResponse(e, "upserting address")
        address
    }
  }
}
