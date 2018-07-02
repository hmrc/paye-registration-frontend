/*
 * Copyright 2018 HM Revenue & Customs
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

package helpers.mocks

import helpers.MockedComponents
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait KeystoreMock {
  this: MockedComponents =>

  def mockKeystoreFetchAndGet[T](key: String, model: Option[T]): OngoingStubbing[Future[Option[T]]] = {
    when(mockKeystoreConnector.fetchAndGet[T](ArgumentMatchers.contains(key))(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[T]]()))
      .thenReturn(Future.successful(model))
  }

  def mockKeystoreCache[T](key: String, regId: String, txId: String, sessionMap: SessionMap): OngoingStubbing[Future[SessionMap]] = {
    when(mockKeystoreConnector.cache(ArgumentMatchers.contains(key), ArgumentMatchers.contains(regId), ArgumentMatchers.contains(txId), ArgumentMatchers.any[T]())(ArgumentMatchers.any(), ArgumentMatchers.any[Format[T]]()))
      .thenReturn(Future.successful(sessionMap))
  }

  def mockKeystoreCacheError[T](key: String, regId: String, txId: String, err: Exception): OngoingStubbing[Future[SessionMap]] = {
    when(mockKeystoreConnector.cache(ArgumentMatchers.contains(key), ArgumentMatchers.contains(regId), ArgumentMatchers.contains(txId), ArgumentMatchers.any[T]())(ArgumentMatchers.any(), ArgumentMatchers.any[Format[T]]()))
      .thenReturn(Future.failed(err))
  }

  def mockKeystoreClear(): OngoingStubbing[Future[Boolean]] = {
    when(mockKeystoreConnector.remove()(ArgumentMatchers.any()))
      .thenReturn(Future.successful(true))
  }

  def mockFetchCurrentProfile(regID: String = "12345"): OngoingStubbing[Future[Option[CurrentProfile]]] = {
    when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any[Format[CurrentProfile]]()))
        .thenReturn(Future.successful(Some(CurrentProfile(
          regID,
          CompanyRegistrationProfile("held", "txId", None, None),
          "ENG",
          payeRegistrationSubmitted = false,
          incorpStatus = None
        ))))
  }
}
