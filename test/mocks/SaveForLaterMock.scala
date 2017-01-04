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

package mocks

import connectors._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

trait SaveForLaterMock {
  this: MockitoSugar =>

  lazy val mockS4LConnector = mock[S4LConnector]

  def mockS4LFetchAndGet[T](formId: String, model: Option[T], mockS4LConnector: S4LConnector = mockS4LConnector): OngoingStubbing[Future[Option[T]]] = {
    when(mockS4LConnector.fetchAndGet[T](Matchers.anyString(), Matchers.contains(formId))(Matchers.any[HeaderCarrier](), Matchers.any[Format[T]]()))
      .thenReturn(Future.successful(model))
  }

  def mockS4LFetchAll(cacheMap: Option[CacheMap], mockS4LConnector: S4LConnector = mockS4LConnector) : OngoingStubbing[Future[Option[CacheMap]]] = {
    when(mockS4LConnector.fetchAll(Matchers.anyString())(Matchers.any[HeaderCarrier]()))
      .thenReturn(Future.successful(cacheMap))
  }

  def mockS4LClear(mockS4LConnector: S4LConnector = mockS4LConnector) : OngoingStubbing[Future[HttpResponse]] = {
    when(mockS4LConnector.clear(Matchers.anyString())(Matchers.any[HeaderCarrier]()))
      .thenReturn(Future.successful(HttpResponse(200)))
  }

  def mockS4LSaveForm[T](formId: String, cacheMap: CacheMap, mockS4LConnector: S4LConnector = mockS4LConnector) : OngoingStubbing[Future[CacheMap]] = {
    when(mockS4LConnector.saveForm[T](Matchers.anyString(), Matchers.contains(formId), Matchers.any[T]())(Matchers.any[HeaderCarrier](), Matchers.any[Format[T]]()))
      .thenReturn(Future.successful(cacheMap))
  }
}
