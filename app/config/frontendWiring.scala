/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.stream.Materializer
import com.typesafe.config.Config
import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.mvc.{Call, Filter, RequestHeader, Result}
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

import scala.concurrent.Future

class FrontendAuditConnector @Inject()(env:Environment, runModeConfiguration: Configuration) extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig(runModeConfiguration,env.mode, s"auditing")
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

class WSHttpImpl @Inject()(override val runModeConfiguration: Configuration, environment: Environment, frontendAuditCon: FrontendAuditConnector,val actorSystem: ActorSystem) extends WSHttp with ServicesConfig {
  override val appName        = getString("appName")
  override val hooks          = NoneRequired
  override def auditConnector = frontendAuditCon
  override protected def mode = environment.mode

  override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
}

class AuthClientConnectorImpl @Inject()(val http: WSHttp, override val runModeConfiguration: Configuration, environment: Environment) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl = baseUrl("auth")
  override protected def mode = environment.mode
}

class PAYEShortLivedHttpCaching @Inject()(val http: WSHttp, override val runModeConfiguration: Configuration, environment: Environment) extends ShortLivedHttpCaching with ServicesConfig {
  override lazy val defaultSource = getString("appName")
  override lazy val baseUri       = baseUrl("cachable.short-lived-cache")
  override lazy val domain        = getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
  override protected def mode = environment.mode
}

class PAYEShortLivedCache @Inject()(val shortLiveCache: ShortLivedHttpCaching,
                                    val cryptoDi: ApplicationCrypto) extends ShortLivedCache {
  override implicit lazy val crypto = cryptoDi.JsonCrypto
}

class PAYESessionCache @Inject()(val http: WSHttp, override val runModeConfiguration: Configuration, environment: Environment) extends SessionCache with ServicesConfig {
  override lazy val defaultSource = getString("appName")
  override lazy val baseUri       = baseUrl("cachable.session-cache")
  override lazy val domain        = getConfString("cachable.session-cache.domain",
    throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
  override protected def mode = environment.mode
}