/*
 * Copyright 2021 HM Revenue & Customs
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

package config

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{AuditingConfigProvider, RunMode}
import uk.gov.hmrc.play.http.ws._

@Singleton
class FrontendAuditConnector @Inject()(configuration: Configuration,
                                       runMode: RunMode,
                                       val materializer: Materializer,
                                       val lifecycle: ApplicationLifecycle) extends AuditConnector {
  override lazy val auditingConfig = new AuditingConfigProvider(configuration, runMode, "auditing").get
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
}

trait WSHttp extends
  HttpGet with WSGet with
  HttpPut with WSPut with
  HttpPatch with WSPatch with
  HttpPost with WSPost with
  HttpDelete with WSDelete with Hooks

class WSHttpImpl @Inject()(appConfig: AppConfig,
                           frontendAuditConnector: FrontendAuditConnector,
                           val actorSystem: ActorSystem,
                           val wsClient: WSClient) extends WSHttp {

  override val appName = appConfig.servicesConfig.getString("appName")
  override val hooks = NoneRequired

  override def auditConnector = frontendAuditConnector

  override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
}

class AuthClientConnectorImpl @Inject()(val http: WSHttp, appConfig: AppConfig) extends PlayAuthConnector {
  override val serviceUrl = appConfig.servicesConfig.baseUrl("auth")
}

class PAYEShortLivedHttpCaching @Inject()(val http: WSHttp, appConfig: AppConfig) extends ShortLivedHttpCaching {
  override lazy val defaultSource = appConfig.servicesConfig.getString("appName")
  override lazy val baseUri = appConfig.servicesConfig.baseUrl("cachable.short-lived-cache")
  override lazy val domain = appConfig.servicesConfig.getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

class PAYEShortLivedCache @Inject()(val shortLiveCache: ShortLivedHttpCaching,
                                    val cryptoDi: ApplicationCrypto) extends ShortLivedCache {
  override implicit lazy val crypto = cryptoDi.JsonCrypto
}

class PAYESessionCache @Inject()(val http: WSHttp, appConfig: AppConfig) extends SessionCache {
  override lazy val defaultSource = appConfig.servicesConfig.getString("appName")
  override lazy val baseUri = appConfig.servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain = appConfig.servicesConfig.getConfString("cachable.session-cache.domain",
    throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}