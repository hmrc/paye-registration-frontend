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

package controllers.internal

import connectors.{IncorporationInformationConnector, KeystoreConnector, PAYERegistrationConnector}
import enums.RegistrationDeletion
import helpers.{PayeComponentSpec, PayeFakedApp}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{CompanyDetailsService, IncorporationInformationService, PAYERegistrationService, S4LService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class RegistrationControllerSpec extends PayeComponentSpec with PayeFakedApp {

  trait Setup{
    val controller = new RegistrationController {
      override val payeRegistrationConnector: PAYERegistrationConnector = mockPayeRegistrationConnector
      override val payeRegistrationService: PAYERegistrationService = mockPayeRegService
      override def redirectToLogin: Result = MockAuthRedirects.redirectToLogin
      override def redirectToPostSign: Result = MockAuthRedirects.redirectToPostSign
      override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
      override val incorporationInformationConnector: IncorporationInformationConnector = mockIncorpInfoConnector
      override def authConnector: AuthConnector = mockAuthConnector
      override def messagesApi: MessagesApi = mockMessagesApi
    }
  }

  "companyIncorporation" should {
    "return a 200" when {
      val responseJson = Json.parse(
        s"""
           |{
           | "SCRSIncorpStatus": {
           |   "IncorpSubscriptionKey" : {
           |     "transactionId" : "fooTxID",
           |     "subscriber"    : "SCRS",
           |     "discriminator" : "paye-fe"
           |   },
           |   "IncorpStatusEvent": {
           |     "status" : "accepted",
           |     "crn" : "12345678",
           |     "description" : "test desc"
           |   }
           | }
           |}
      """.stripMargin)

      "all data is passed in and data is deleted" in new Setup{
        when(mockPayeRegService.handleIIResponse(ArgumentMatchers.eq("fooTxID"), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.success))

        AuthHelpers.submitUnauthorisedT[JsValue](controller.companyIncorporation, FakeRequest().withBody(responseJson)){
          res => status(res) mustBe OK
        }
      }
    }

    "return an internal server error" when {
      "unexpected json is recieved" in new Setup {
        val responseJson = Json.parse(
          s"""
             |{
             | "SCRSIncorpStatus": {
             |   "IncorpSubscriptionKey" : {
             |     "transactionId" : "fooTxID",
             |     "subscriber"    : "SCRS",
             |     "discriminator" : "paye-fe"
             |   },
             |   "IncorpStatusEvent": {
             |     "status" : "dismembered",
             |     "crn" : "12345678",
             |     "description" : "test desc"
             |   }
             | }
             |}
      """.stripMargin)

        AuthHelpers.submitUnauthorisedT[JsValue](controller.companyIncorporation, FakeRequest().withBody(responseJson)){
          res => status(res) mustBe 500
        }
      }
      "deletion is unsuccesful with generic exception" in new Setup {
        val responseJson = Json.parse(
          s"""
             |{
             | "SCRSIncorpStatus": {
             |   "IncorpSubscriptionKey" : {
             |     "transactionId" : "fooTxID",
             |     "subscriber"    : "SCRS",
             |     "discriminator" : "paye-fe"
             |   },
             |   "IncorpStatusEvent": {
             |     "status" : "accepted",
             |     "crn" : "12345678",
             |     "description" : "test desc"
             |   }
             | }
             |}
      """.stripMargin)

        when(mockPayeRegService.handleIIResponse(ArgumentMatchers.eq("fooTxID"), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("ouch it hurts")))

        AuthHelpers.submitUnauthorisedT[JsValue](controller.companyIncorporation, FakeRequest().withBody(responseJson)){
          res => status(res) mustBe 500
        }
      }
      "deletion is not success and returns 404" in new Setup {
        val responseJson = Json.parse(
          s"""
             |{
             | "SCRSIncorpStatus": {
             |   "IncorpSubscriptionKey" : {
             |     "transactionId" : "fooTxID",
             |     "subscriber"    : "SCRS",
             |     "discriminator" : "paye-fe"
             |   },
             |   "IncorpStatusEvent": {
             |     "status" : "accepted",
             |     "crn" : "12345678",
             |     "description" : "test desc"
             |   }
             | }
             |}
      """.stripMargin)

        when(mockPayeRegService.handleIIResponse(ArgumentMatchers.eq("fooTxID"), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(RegistrationDeletion.notfound))

        AuthHelpers.submitUnauthorisedT[JsValue](controller.companyIncorporation, FakeRequest().withBody(responseJson)){
          res => status(res) mustBe 200
        }
      }
    }
  }
}