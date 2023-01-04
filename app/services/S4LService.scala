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

package services

import connectors.S4LConnector
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Formatters

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class S4LServiceImpl @Inject()(val s4LConnector: S4LConnector)(implicit val ec: ExecutionContext) extends S4LService

trait S4LService {
  implicit val ec: ExecutionContext
  val s4LConnector: S4LConnector

  def saveForm[T](formId: String, data: T, regId: String)(implicit hc: HeaderCarrier, format: Format[T], request: Request[_]): Future[CacheMap] = {
    for {
      cacheMap <- s4LConnector.saveForm[T](regId, formId, data)
    } yield cacheMap
  }

  def saveIntMap[V](formId: String, data: Map[Int, V], regId: String)(implicit hc: HeaderCarrier, formatV: Format[V], request: Request[_]): Future[CacheMap] = {
    implicit val mapFormat: Format[Map[Int, V]] = Format(Formatters.intMapReads[V], Formatters.intMapWrites[V])
    for {
      cacheMap <- s4LConnector.saveForm[Map[Int, V]](regId, formId, data)
    } yield cacheMap
  }

  def fetchAndGet[T](formId: String, regId: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[Option[T]] = {
    for {
      data <- s4LConnector.fetchAndGet[T](regId, formId)
    } yield data
  }

  def fetchAndGetIntMap[V](formId: String, regId: String)(implicit hc: HeaderCarrier, formatV: Format[V]): Future[Option[Map[Int, V]]] = {
    implicit val mapFormat: Format[Map[Int, V]] = Format(Formatters.intMapReads[V], Formatters.intMapWrites[V])
    for {
      cacheMap <- s4LConnector.fetchAndGet[Map[Int, V]](regId, formId)
    } yield cacheMap
  }

  def clear(regId: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[HttpResponse] = {
    for {
      resp <- s4LConnector.clear(regId)
    } yield resp
  }

  def fetchAll(regId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    for {
      cacheMap <- s4LConnector.fetchAll(regId)
    } yield cacheMap
  }
}
