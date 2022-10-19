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

import common.exceptions
import common.exceptions.DownstreamExceptions
import config.AppConfig
import models.external.CompanyRegistrationProfile
import play.api.libs.json._
import services.MetricsService
import uk.gov.hmrc.http.{BadRequestException, CoreGet, HeaderCarrier, HttpClient, HttpException}
import utils.{PAYEFeatureSwitch, PAYEFeatureSwitches}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyRegistrationConnector @Inject()(val featureSwitch: PAYEFeatureSwitch,
                                             val http: HttpClient,
                                             val metricsService: MetricsService,
                                             appConfig: AppConfig)(implicit val ec: ExecutionContext) {

  lazy val companyRegistrationUrl: String = appConfig.servicesConfig.baseUrl("company-registration")
  lazy val companyRegistrationUri: String = appConfig.servicesConfig.getString("microservice.services.company-registration.uri")
  lazy val stubUrl: String = appConfig.servicesConfig.baseUrl("incorporation-frontend-stubs")
  lazy val stubUri: String = appConfig.servicesConfig.getString("microservice.services.incorporation-frontend-stubs.uri")

  def getCompanyRegistrationDetails(regId: String)(implicit hc: HeaderCarrier): Future[CompanyRegistrationProfile] = {
    val companyRegTimer = metricsService.companyRegistrationResponseTimer.time()

    val url = if (useCompanyRegistration) s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration" else s"$stubUrl$stubUri"

    http.GET[JsObject](s"$url/$regId/corporation-tax-registration") map { response =>
      companyRegTimer.stop()
      val status = (response \ "status").as[String]
      val txId = (response \ "confirmationReferences" \ "transaction-id").validate[String].fold(
        _ => throw new exceptions.DownstreamExceptions.ConfirmationRefsNotFoundException,
        identity
      )
      val paidIncorporation = (response \ "confirmationReferences" \ "payment-reference").asOpt[String]
      val ackStatus = (response \ "acknowledgementReferences" \ "status").asOpt[String]
      CompanyRegistrationProfile(status, txId, ackStatus, paidIncorporation)
    } recover {
      case badRequestErr: BadRequestException =>
        companyRegTimer.stop()
        logger.error(s"[getCompanyRegistrationDetails] Received a BadRequest status code when expecting a Company Registration document for reg id: $regId")
        throw badRequestErr
      case noRefsErr: DownstreamExceptions.ConfirmationRefsNotFoundException =>
        companyRegTimer.stop()
        logger.error(s"[getCompanyRegistrationDetails] Received an error when expecting a Company Registration document for reg id: $regId could not find confirmation references (has user completed Incorp/CT?)")
        throw noRefsErr
      case ex: Exception =>
        companyRegTimer.stop()
        logger.error(s"[getCompanyRegistrationDetails] Received an error when expecting a Company Registration document for reg id: $regId error: ${ex.getMessage}")
        throw ex
    }
  }

  def getVerifiedEmail(regId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val emailRetrieveURL = s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration/$regId/retrieve-email"

    http.GET[JsObject](emailRetrieveURL).map(js => Some((js \ "address").as[String]))
      .recover {
        case e: HttpException =>
          logger.warn(s"[getVerifiedEmail] A call was made to company reg and an unsuccessful response was returned for regId: $regId and message: ${e.getMessage}")
          None
        case e: Exception =>
          logger.error(s"[getVerifiedEmail] An unexpected exception occurred for regId: $regId and message: ${e.getMessage}")
          None
      }
  }

  private[connectors] def useCompanyRegistration: Boolean = featureSwitch.companyReg.enabled
}