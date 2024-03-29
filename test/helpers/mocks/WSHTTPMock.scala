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

package helpers.mocks

import helpers.MockedComponents
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}

import scala.concurrent.Future

trait WSHTTPMock {
  this: MockedComponents =>

  def mockHttpGet[T](url: String, thenReturn: T): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpGet[T](url: String, thenReturn: Future[T]): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(thenReturn)
  }

  def mockHttpPOST[I, O](url: String, thenReturn: O, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.POST[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpPOSTForm[O](url: String, thenReturn: O, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.POSTForm[O](ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpPUT[I, O](url: String, thenReturn: O, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.PUT[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpPATCH[I, O](url: String, thenReturn: O, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.PATCH[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }


  def mockHttpFailedGET[T](url: String, exception: Exception): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](ArgumentMatchers.anyString(),ArgumentMatchers.any(),ArgumentMatchers.any()
    )(ArgumentMatchers.any[HttpReads[T]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpFailedPOST[I, O](url: String, exception: Exception, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.POST[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpFailedPATCH[I, O](url: String, exception: Exception, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.PATCH[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpFailedPUT[I, O](url: String, exception: Exception, mockHttpClient: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.PUT[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpDelete[O](thenReturn: O): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.DELETE[O](ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(thenReturn))
  }

  def mockHttpFailedDelete[O](exception: Exception): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.DELETE[O](ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(exception))
  }
}
