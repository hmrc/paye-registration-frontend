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

import com.codahale.metrics.{Counter, Timer}
import config.WSHttp
import models.Address
import models.external.{AddressLookupFrontendConf, ConfirmPage, EditPage, LookupPage, SelectPage, Timeout}
import play.api.i18n.MessagesApi
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import services.{MetricsService, MetricsSrv}
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupConnector @Inject()(val metricsService: MetricsService,
                                       val messagesApi: MessagesApi,
                                       config: ServicesConfig,
                                       playConfig: Configuration) extends AddressLookupConnect {
  val addressLookupFrontendUrl     = config.baseUrl("address-lookup-frontend")
  lazy val payeRegistrationUrl     = config.getConfString("paye-registration-frontend.www.url","")
  val http : CoreGet with CorePost = WSHttp
  val successCounter = metricsService.addressLookupSuccessResponseCounter
  val failedCounter  = metricsService.addressLookupFailedResponseCounter
  def timer          = metricsService.addressLookupResponseTimer.time()
  lazy val timeoutAmount: Int = playConfig.underlying.getInt("timeoutInSeconds")
}

class ALFLocationHeaderNotSetException extends NoStackTrace

trait AddressLookupConnect {

  val addressLookupFrontendUrl: String
  val payeRegistrationUrl: String
  val http: CoreGet with CorePost
  val metricsService: MetricsSrv
  val messagesApi: MessagesApi

  val successCounter: Counter
  val failedCounter: Counter
  def timer: Timer.Context

  val timeoutAmount: Int

  def getAddress(id: String)(implicit hc: HeaderCarrier) = {
    implicit val reads = Address.adressLookupReads
    metricsService.processDataResponseWithMetrics[Address](successCounter, failedCounter, timer) {
      http.GET[Address](s"$addressLookupFrontendUrl/api/confirmed?id=$id")
    }
  }

  private[connectors] def createOnRampJson(key: String, call: Call): JsObject = {
    val showBackButtons: Boolean = true
    val showPhaseBanner: Boolean = true
    val includeHMRCBranding: Boolean = false
    val proposalListLimit: Int = 20
    val showSearchAgainLink: Boolean = true
    val showChangeLink: Boolean = true
    val showSubHeadingAndInfo: Boolean = false

    val conf = AddressLookupFrontendConf(
      continueUrl = s"$payeRegistrationUrl${call.url}",
      navTitle = messagesApi("pages.alf.common.navTitle"),
      showPhaseBanner = showPhaseBanner,
      phaseBannerHtml = messagesApi("pages.alf.common.phaseBannerHtml"),
      showBackButtons = showBackButtons,
      includeHMRCBranding = includeHMRCBranding,
      deskProServiceName = messagesApi("pages.alf.common.deskProServiceName"),
      lookupPage = LookupPage(
        title = messagesApi(s"pages.alf.$key.lookupPage.title"),
        heading = messagesApi(s"pages.alf.common.lookupPage.heading"),
        filterLabel = messagesApi(s"pages.alf.common.lookupPage.filterLabel"),
        submitLabel = messagesApi(s"pages.alf.common.lookupPage.submitLabel")
      ),
      selectPage = SelectPage(
        title = messagesApi(s"pages.alf.common.selectPage.description"),
        heading = messagesApi(s"pages.alf.common.selectPage.description"),
        proposalListLimit = proposalListLimit,
        showSearchAgainLink = showSearchAgainLink
      ),
      editPage = EditPage(
        title = messagesApi(s"pages.alf.common.editPage.description"),
        heading = messagesApi(s"pages.alf.common.editPage.description"),
        line1Label = messagesApi(s"pages.alf.common.editPage.line1Label"),
        line2Label = messagesApi(s"pages.alf.common.editPage.line2Label"),
        line3Label = messagesApi(s"pages.alf.common.editPage.line3Label"),
        showSearchAgainLink = showSearchAgainLink
      ),
      confirmPage = ConfirmPage(
        title = messagesApi(s"pages.alf.common.confirmPage.title"),
        heading = messagesApi(s"pages.alf.$key.confirmPage.heading"),
        showSubHeadingAndInfo = showSubHeadingAndInfo,
        submitLabel = messagesApi(s"pages.alf.common.confirmPage.submitLabel"),
        showChangeLink = showChangeLink,
        changeLinkText = messagesApi(s"pages.alf.common.confirmPage.changeLinkText")
      ),
      timeout = Timeout(
        timeoutAmount = timeoutAmount,
        timeoutUrl = s"$payeRegistrationUrl${controllers.userJourney.routes.SignInOutController.destroySession().url}"
      )
    )

    Json.toJson(conf).as[JsObject]
  }

  def getOnRampUrl(key: String, call: Call)(implicit hc: HeaderCarrier): Future[String] = {
    val postUrl      = s"$addressLookupFrontendUrl/api/init"
    val continueJson = createOnRampJson(key, call)

    metricsService.processDataResponseWithMetrics(successCounter, failedCounter, timer) {
      http.POST[JsObject, HttpResponse](postUrl, continueJson)
    } map {
      _.header("Location").getOrElse {
        Logger.warn("[AddressLookupConnector] [getOnRampUrl] - ERROR: Location header not set in ALF response")
        throw new ALFLocationHeaderNotSetException
      }
    }
  }
}
