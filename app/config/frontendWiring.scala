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

package config

import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}

import javax.inject.Inject

class PAYEShortLivedHttpCaching @Inject()(val http: HttpClient, appConfig: AppConfig) extends ShortLivedHttpCaching {
  override lazy val defaultSource: String = appConfig.servicesConfig.getString("appName")
  override lazy val baseUri: String = appConfig.servicesConfig.baseUrl("cachable.short-lived-cache")
  override lazy val domain: String = appConfig.servicesConfig.getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

class PAYEShortLivedCache @Inject()(val shortLiveCache: PAYEShortLivedHttpCaching,
                                    val cryptoDi: ApplicationCrypto) extends ShortLivedCache {
  override implicit lazy val crypto = cryptoDi.JsonCrypto
}

class PAYESessionCache @Inject()(val http: HttpClient, appConfig: AppConfig) extends SessionCache {
  override lazy val defaultSource: String = appConfig.servicesConfig.getString("appName")
  override lazy val baseUri: String = appConfig.servicesConfig.baseUrl("cachable.session-cache")
  override lazy val domain: String = appConfig.servicesConfig.getConfString("cachable.session-cache.domain",
    throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}