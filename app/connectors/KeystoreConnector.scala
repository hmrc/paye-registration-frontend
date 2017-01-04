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

import config.PAYESessionCache
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.{SessionCache, CacheMap}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

object KeystoreConnector extends KeystoreConnector

trait KeystoreConnector {
  val sessionCache: SessionCache = PAYESessionCache

  def cache[T](formId: String, body : T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    sessionCache.cache[T](formId, body)
  }

  def fetch()(implicit hc : HeaderCarrier) : Future[Option[CacheMap]] = {
    sessionCache.fetch()
  }

  def fetchAndGet[T](key : String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    sessionCache.fetchAndGetEntry(key)
  }

  def remove()(implicit hc : HeaderCarrier) : Future[HttpResponse] = {
    sessionCache.remove()
  }


}
