/*
 * Copyright 2016 HM Revenue & Customs
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

import config.PAYEShortLivedCache
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, CacheMap}
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}

import scala.concurrent.Future

object S4LConnector extends S4LConnector

trait S4LConnector {

  val shortCache : ShortLivedCache = PAYEShortLivedCache

  def saveForm[T](userId: String, formId: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    shortCache.cache[T](userId, formId, data)
  }

  def fetchAndGet[T](userId: String, formId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    shortCache.fetchAndGetEntry[T](userId, formId)
  }

  def clear(userId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    shortCache.remove(userId)
  }

  def fetchAll(userId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    shortCache.fetch(userId)
  }
}
