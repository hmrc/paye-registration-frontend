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

package controllers.test

import builders.AuthBuilder
import connectors.test.{TestBusinessRegConnect, TestIncorpInfoConnect, TestPAYERegConnect}
import enums.DownstreamOutcome
import models.external.BusinessProfile
import models.test.CoHoCompanyDetailsFormModel
import play.api.http.Status
import play.api.mvc.{AnyContent, Request}
import services.{IncorporationInformationSrv, PAYERegistrationSrv, S4LSrv}
import testHelpers.PAYERegSpec

import scala.concurrent.Future

class TestSetupControllerSpec extends PAYERegSpec {
  val mockTestBusRegConnector = mock[TestBusinessRegConnect]
  val mockCoHoAPIService = mock[IncorporationInformationSrv]
  val mockTestAPIConnector = mock[TestIncorpInfoConnect]
  val mockPayeRegConnector = mock[TestPAYERegConnect]
  val mockPayeRegService = mock[PAYERegistrationSrv]
  val mockS4LService = mock[S4LSrv]

  class Setup {
    val controller = new TestSetupCtrl {
      override val businessRegConnector = mockBusinessRegistrationConnector
      override val keystoreConnector = mockKeystoreConnector
      override val testBusinessRegConnector = mockTestBusRegConnector
      override val authConnector = mockAuthConnector
      override val testIncorpInfoConnector = mockTestAPIConnector
      override val coHoAPIService = mockCoHoAPIService
      override val messagesApi = mockMessages
      override val payeRegService = mockPayeRegService
      override val testPAYERegConnector = mockPayeRegConnector
      override val s4LService = mockS4LService

      override def doBusinessProfileSetup(implicit request: Request[AnyContent]): Future[BusinessProfile] = Future.successful(BusinessProfile("regId", None, "en"))
      override def doCoHoCompanyDetailsTearDown(regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
      override def doAddCoHoCompanyDetails(formModel: CoHoCompanyDetailsFormModel, regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
      override def doIndividualRegTeardown(regId: String) (implicit request: Request[AnyContent]): Future[DownstreamOutcome.Value] = Future.successful(DownstreamOutcome.Success)
      override def doTearDownS4L(regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
      override def doTeardownOfficers()(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
      override def doSetupOfficers(regId: String)(implicit request: Request[AnyContent]): Future[String] = Future.successful("test")
    }
  }
  
  "setup" should {
    "redirect to post sign in" in new Setup {
      mockFetchCurrentProfile()
      AuthBuilder.showWithAuthorisedUser(controller.testSetup("TESTLTD"), mockAuthConnector) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye"
      }
    }
  }
}
