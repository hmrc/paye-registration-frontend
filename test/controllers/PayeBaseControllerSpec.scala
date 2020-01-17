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

package controllers

import connectors.IncorporationInformationConnector
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, InsufficientConfidenceLevel}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class PayeBaseControllerSpec extends PayeComponentSpec with PayeFakedApp {

  class Setup {
    val testBaseController = new PayeBaseController {
      override val redirectToLogin         = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign      = MockAuthRedirects.redirectToPostSign
      override val messagesApi             = mockMessagesApi
      override val authConnector           = mockAuthConnector
      override val keystoreConnector       = mockKeystoreConnector
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService
    }

    def testOkFunction(msg: String): Future[Result] = Future.successful(Ok(msg))
  }

  "isAuthorised" should {
    "return an ok" when {
      "the user has been successfully authorised" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        val result: Future[Result] = testBaseController.isAuthorised { request =>
          testOkFunction("user is authorised")
        }(request)

        status(result) mustBe OK
        contentAsString(result) mustBe "user is authorised"
      }
    }

    "return a SeeOther and redirect to sign in" when {
      "the user is not authorised (redirect to sign in)" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new InsufficientConfidenceLevel))

        val result: Future[Result] = testBaseController.isAuthorised { request =>
          testOkFunction("user is authorised")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/test/login")
      }
    }
  }

  "isAuthorisedWithProfile" should {
    "return an Ok" when {
      "the user is authorised and the user CurrentProfile has been fetch" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Fixtures.validCurrentProfile))

        val result: Future[Result] = testBaseController.isAuthorisedWithProfile {
          request =>
            profile =>
              testOkFunction("user is authorised with profile")
        }(request)

        status(result) mustBe OK
        contentAsString(result) mustBe "user is authorised with profile"
      }
    }

    "return a SeeOther" when {
      "the user is authorised but the user doesn't have a CurrentProfile (redirects to PAYE start)" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        when(mockKeystoreConnector.fetchAndGetFromKeystore(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = testBaseController.isAuthorisedWithProfile {
          request =>
            profile =>
              testOkFunction("user is authorised with profile")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/register-for-paye/start-pay-as-you-earn")
      }

      "the user not authorised (redirect to sign in)" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new InsufficientConfidenceLevel))

        val result: Future[Result] = testBaseController.isAuthorisedWithProfile {
          request =>
            profile =>
              testOkFunction("user is authorised with profile")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/test/login")
      }
    }
  }

  "isAuthorisedAndIsOrg" should {
    "return an Ok" when {
      "the user is authorised and the user affinity group is organisation" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(Organisation)))

        val result: Future[Result] = testBaseController.isAuthorisedAndIsOrg { request =>
          testOkFunction("user is authorised with org affinity")
        }(request)

        status(result) mustBe OK
        contentAsString(result) mustBe "user is authorised with org affinity"
      }
    }

    "return a SeeOther" when {
      "the user is authorised but is an Individual account" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(Individual)))

        val result: Future[Result] = testBaseController.isAuthorisedAndIsOrg { request =>
          testOkFunction("user is authorised with org affinity")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }

      "the user is authorised but has no affinity" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = testBaseController.isAuthorisedAndIsOrg { request =>
          testOkFunction("user is authorised with org affinity")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://www.tax.service.gov.uk/business-registration/select-taxes")
      }

      "the user is not authorised" in new Setup {
        val request = FakeRequest("GET", "/test/authorised-action")

        when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(new InsufficientConfidenceLevel))

        val result: Future[Result] = testBaseController.isAuthorisedAndIsOrg { request =>
          testOkFunction("user is authorised with org affinity")
        }(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/test/login")
      }
    }
  }
}