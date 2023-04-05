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

package controllers.test

import config.AppConfig
import enums.DownstreamOutcome
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.external.BusinessProfile
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future

class TestSetupControllerSpec extends PayeComponentSpec with PayeFakedApp {
  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val config: AppConfig = app.injector.instanceOf[AppConfig]

  class Setup extends CodeMocks {
    val controller: TestSetupController = new TestSetupController(
      testIncorpInfoConnector = mockTestIncorpInfoConnector,
      coHoAPIService = mockIncorpInfoService,
      testPAYERegConnector = mockTestPayeRegConnector,
      payeRegService = mockPayeRegService,
      businessProfileController = mockBusinessProfileController,
      appConfig = config,
      testBusinessRegConnector = mockTestBusRegConnector,
      authConnector = mockAuthConnector,
      payeRegistrationService = mockPayeRegService,
      keystoreConnector = mockKeystoreConnector,
      incorporationInformationConnector = mockIncorpInfoConnector,
      mcc = mockMcc,
      businessRegConnector = mockBusinessRegistrationConnector,
      s4LService = mockS4LService
    ) {

//      override def businessProfileSetup(implicit request: Request[AnyContent]): Future[BusinessProfile] = Future.successful(BusinessProfile("regId", "en"))

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

      when(mockBusinessProfileController.doBusinessProfileSetup(any())).thenReturn(Future.successful(BusinessProfile("regId", "en")))

      val result: Future[Result] = controller.testSetup("TESTLTD")(FakeRequest())
      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result) mustBe Some("/register-for-paye")
    }
  }

  "update-status" should {
    "return 200 for success" in new Setup {
      mockFetchCurrentProfile()
      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(()))

      when(mockBusinessProfileController.doBusinessProfileSetup(any())).thenReturn(Future.successful(BusinessProfile("regId", "en")))
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(any(), any()))
        .thenReturn(Future.successful(BusinessProfile("regId", "en")))

      when(mockTestPayeRegConnector.updateStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Success))

      val result: Future[Result] = controller.updateStatus("draft")(FakeRequest())
      status(result) mustBe OK
    }

    "return 500 for failure" in new Setup {
      mockFetchCurrentProfile()

      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(()))

      when(mockBusinessProfileController.doBusinessProfileSetup(any())).thenReturn(Future.successful(BusinessProfile("regId", "en")))
      when(mockBusinessRegistrationConnector.retrieveCurrentProfile(any(), any()))
        .thenReturn(Future.successful(BusinessProfile("regId", "en")))

      when(mockTestPayeRegConnector.updateStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(DownstreamOutcome.Failure))

      val result: Future[Result] = controller.updateStatus("draft")(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}