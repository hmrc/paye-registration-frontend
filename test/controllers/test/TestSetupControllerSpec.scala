/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.test

import config.AppConfig
import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import play.api.test.FakeRequest
import scala.concurrent.Future

class TestSetupControllerSpec extends PayeComponentSpec with PayeFakedApp {
  lazy val mockMcc = app.injector.instanceOf[MessagesControllerComponents]
  class Setup extends CodeMocks {
    val controller = new TestSetupController(mockMcc) {
      override val appConfig: AppConfig = mockAppConfig
      override val redirectToLogin = MockAuthRedirects.redirectToLogin
      override val redirectToPostSign = MockAuthRedirects.redirectToPostSign

      override val businessRegConnector = mockBusinessRegistrationConnector
      override val keystoreConnector = mockKeystoreConnector
      override val testBusinessRegConnector = mockTestBusRegConnector
      override val authConnector = mockAuthConnector
      override val testIncorpInfoConnector = mockTestIncorpInfoConnector
      override val coHoAPIService = mockIncorpInfoService
      override val messagesApi = mockMessagesApi
      override val payeRegService = mockPayeRegService
      override val testPAYERegConnector = mockTestPayeRegConnector
      override val s4LService = mockS4LService
      override val incorporationInformationConnector = mockIncorpInfoConnector
      override val payeRegistrationService = mockPayeRegService

      override def doBusinessProfileSetup(implicit request: Request[AnyContent]): Future[BusinessProfile] = Future.successful(BusinessProfile("regId", "en"))

      override def doCoHoCompanyDetailsTearDown(regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")

      override def doAddCoHoCompanyDetails(regId: String, companyName: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")

      override def doIndividualRegTeardown(regId: String)(implicit request: Request[AnyContent]): Future[DownstreamOutcome.Value] = Future.successful(DownstreamOutcome.Success)

      override def doTearDownS4L(regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
    }
  }

  "setup" should {
    "redirect to post sign in" in new Setup {
      mockFetchCurrentProfile()

      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(()))

      when(mockTestBusRegConnector.updateCompletionCapacity(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful("director"))

      val result = controller.testSetup("TESTLTD")(FakeRequest())
      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result) mustBe Some("/register-for-paye")
    }
  }

  "update-status" should {
    "return 200 for success" in new Setup {
      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(()))

      mockBusinessRegFetch(Future.successful(BusinessProfile("regID", "EN")))

      when(mockTestPayeRegConnector.updateStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      val result = controller.updateStatus("draft")(FakeRequest())
      status(result) mustBe OK
    }

    "return 500 for failure" in new Setup {
      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(()))

      mockBusinessRegFetch(Future.successful(BusinessProfile("regID", "EN")))

      when(mockTestPayeRegConnector.updateStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      val result = controller.updateStatus("draft")(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}