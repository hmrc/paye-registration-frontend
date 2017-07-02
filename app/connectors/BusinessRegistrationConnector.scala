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

package connectors

import javax.inject.{Inject, Singleton}

import config.WSHttp
import models.Address
import models.external.BusinessProfile
import models.view.PAYEContactDetails
import play.api.Logger
import play.api.libs.json.JsValue
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessRegistrationConnector @Inject()(injMetrics: MetricsService) extends BusinessRegistrationConnect with ServicesConfig {
  val businessRegUrl = baseUrl("business-registration")
  val http : WSHttp = WSHttp
  val metricsService = injMetrics
}

trait BusinessRegistrationConnect {

  val businessRegUrl: String
  val http: WSHttp
  val metricsService: MetricsSrv

  def retrieveCurrentProfile(implicit hc: HeaderCarrier, rds: HttpReads[BusinessProfile]): Future[BusinessProfile] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration") map {
      profile =>
        businessRegistrationTimer.stop()
        profile
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        throw logResponse(e, "retrieveCompletionCapacity", "retrieving completion capacity")
    }
  }

  def retrieveCompletionCapacity(implicit hc: HeaderCarrier, rds: HttpReads[JsValue]): Future[Option[String]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    http.GET[JsValue](s"$businessRegUrl/business-registration/business-tax-registration") map {
      json =>
        businessRegistrationTimer.stop()
        (json \ "completionCapacity").asOpt[String]
    } recover {
      case e =>
        businessRegistrationTimer.stop()
        throw logResponse(e, "retrieveCompletionCapacity", "retrieving completion capacity")
    }
  }

  def retrieveContactDetails(regId: String)(implicit hc: HeaderCarrier, rds: HttpReads[PAYEContactDetails]): Future[Option[PAYEContactDetails]] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val rds = PAYEContactDetails.prepopReads
    http.GET[PAYEContactDetails](s"$businessRegUrl/business-registration/$regId/contact-details") map {
      details =>
        businessRegistrationTimer.stop()
        Some(details)
    } recover {
      case notfound: NotFoundException =>
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
    http.POST[PAYEContactDetails, JsValue](s"$businessRegUrl/business-registration/$regId/contact-details", contactDetails) map {
      _ =>
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
    http.GET[JsValue](s"$businessRegUrl/business-registration/$regId/addresses") map {
      json =>
        businessRegistrationTimer.stop()
        json.\("addresses").as[Seq[JsValue]].map(_.as[Address](Address.prePopReads))
    } recover {
      case ex =>
        businessRegistrationTimer.stop()
        logResponse(ex, "retrieveAddresses", "fetching addresses from pre-pop", Some(regId))
        Seq.empty
    }
  }

  def upsertAddress(regId: String, address: Address)(implicit hc: HeaderCarrier): Future[Address] = {
    val businessRegistrationTimer = metricsService.businessRegistrationResponseTimer.time()
    implicit val wts = Address.prePopWrites
    http.POST[Address, JsValue](s"$businessRegUrl/business-registration/$regId/addresses", address) map {
      _ =>
        businessRegistrationTimer.stop()
        address
    } recover {
      case e: Exception =>
        businessRegistrationTimer.stop()
        logResponse(e, "upsertAddress", "upserting address")
        address
    }
  }

  private[connectors] def logResponse(e: Throwable, f: String, m: String, regId: Option[String] = None): Throwable = {
    val optRegId = regId.map(r => s" and regId: $regId").getOrElse("")
    def log(s: String) = Logger.error(s"[BusinessRegistrationConnector] [$f] received $s when $m$optRegId")
    e match {
      case e: NotFoundException => log("NOT FOUND")
      case e: BadRequestException => log("BAD REQUEST")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
      case 403 => log("FORBIDDEN")
      case _ => log(s"Upstream 4xx: ${e.upstreamResponseCode} ${e.message}")
    }
      case e: Upstream5xxResponse => log(s"Upstream 5xx: ${e.upstreamResponseCode}")
      case e: Exception => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}
