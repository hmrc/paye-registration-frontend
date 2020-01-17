/*
 * Copyright 2020 HM Revenue & Customs
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

import helpers.PayeComponentSpec
import models.external.EmailRequest
import uk.gov.hmrc.http.{CorePost, HttpException, HttpResponse}
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.Future

class EmailConnectorSpec extends PayeComponentSpec {

  class Setup {
    val emailConn = new EmailConnector {
      override val http: CorePost = mockWSHttp
      override val sendEmailURL: String = "FOOBARWIZZ"
    }
    val validEmailRequest = EmailRequest(
      to = "foo@foo.com" :: Nil,
      templateId = "fooBarWizzId",
      parameters = Map.empty,
      force = false
    )
  }

  "requestEmailToBeSent" should {
    "return an EmailSent" when {
      "call to email service returns a non success code" in new Setup {
        when(mockWSHttp.POST[EmailRequest,HttpResponse](same("FOOBARWIZZ"),same(validEmailRequest),any())(any(),any(),any(),any()))
          .thenReturn(Future.successful(HttpResponse(200, None, Map.empty, None)))

        val res  = await(emailConn.requestEmailToBeSent(validEmailRequest))
        res mustBe EmailSent

        verify(mockWSHttp, times(1)).POST[EmailRequest,HttpResponse](any(),any(),any())(any(),any(),any(),any())
      }
    }

    "return an EmailDifficulties" when {
      "call to email service returns a non success code" in new Setup {
        when(mockWSHttp.POST[EmailRequest,HttpResponse](same("FOOBARWIZZ"),same(validEmailRequest),any())(any(),any(),any(),any()))
          .thenReturn(Future.failed(new HttpException("", 502)))

        val res  = await(emailConn.requestEmailToBeSent(validEmailRequest))
        res mustBe EmailDifficulties

        verify(mockWSHttp, times(1)).POST[EmailRequest,HttpResponse](any(),any(),any())(any(),any(),any(),any())
      }
    }
  }
}
