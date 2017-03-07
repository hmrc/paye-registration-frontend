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

package services

import javax.inject.{Inject, Singleton}

import connectors.{KeystoreConnector, S4LConnect, S4LConnector}
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class S4LService @Inject()(s4LConn: S4LConnector, keystoreConn: KeystoreConnector) extends S4LSrv {
  override val s4LConnector = s4LConn
  override val keystoreConnector = keystoreConn
}

trait S4LSrv extends CommonService {

  val s4LConnector: S4LConnect

  def saveForm[T](formId: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    for {
      regId    <- fetchRegistrationID
      cacheMap <- s4LConnector.saveForm[T](regId, formId, data)
    } yield cacheMap
  }

  def fetchAndGet[T](formId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    for {
      regId <- fetchRegistrationID
      data  <- s4LConnector.fetchAndGet[T](regId, formId)
    } yield data
  }


  def clear()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    for {
      regId <- fetchRegistrationID
      resp <- s4LConnector.clear(regId)
    } yield resp
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    for {
      regId <- fetchRegistrationID
      cacheMap <- s4LConnector.fetchAll(regId)
    } yield cacheMap
  }

}
