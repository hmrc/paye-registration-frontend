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

package helpers

import akka.util.Timeout
import helpers.auth.AuthHelpers
import helpers.fixtures._
import helpers.mocks.internal.BusinessRegistrationConnectorMock
import helpers.mocks.{KeystoreMock, SaveForLaterMock, WSHTTPMock}
import models.external.{AuditingInformation, CurrentProfile}
import org.mockito.Mockito.reset
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.{HeaderNames, HttpProtocol, MimeTypes, Status}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Request, Result}
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future
import scala.concurrent.duration._

trait PayeComponentSpec
  extends PlaySpec
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with HeaderNames
    with Status
    with MimeTypes
    with HttpProtocol
    with DefaultAwaitTimeout
    with ResultExtractors
    with Writeables
    with EssentialActionCaller
    with RouteInvokers
    with FutureAwaits
    with MockedComponents
    with JsonFormValidation {

  override implicit def defaultAwaitTimeout: Timeout = 5.seconds

  implicit val cp: CurrentProfile = Fixtures.validCurrentProfile.get
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")))

  implicit val auditInformation: AuditingInformation = AuditingInformation("testExternalId", "testAuthProviderId")

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def fakeRequest(method: String = "GET") = FakeRequest(method, "")

  private def resetMocks(): Unit = {
    reset(
      mockAppConfig,
      mockPAYEContactService,
      mockPrepopService,
      mockAddressLookupService,
      mockTestPayeRegConnector,
      mockPayeRegService,
      mockTestIncorpInfoConnector,
      mockFeatureManager,
      mockTestBusRegConnector,
      mockCurrentProfileService,
      mockAuditService,
      mockPrepopulationService,
      mockCompRegConnector,
      mockEmailConnector,
      mockEmailService,
      mockAuditConnector,
      mockAddressLookupConnector,
      mockSessionCache,
      mockFeatureSwitch,
      mockFeatureSwitches,
      mockPAYERegConnector,
      mockKeystoreConnector,
      mockPayeRegistrationConnector,
      mockAuthConnector,
      mockOldAuthConnector,
      mockBusinessRegistrationConnector,
      mockS4LConnector,
      mockHttpClient,
      mockIncorpInfoService,
      mockCompanyDetailsService,
      mockS4LService,
      mockThresholdService,
      mockEmploymentService,
      mockConfirmationService,
      mockSessionRepository
    )
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    resetMocks()
  }

  object AuthHelpers extends AuthHelpers {
    override val authConnector = mockAuthConnector
    override val keystoreConnector = mockKeystoreConnector
  }

  object Fixtures
    extends BusinessRegistrationFixture
      with CoHoAPIFixture
      with KeystoreFixture
      with PAYERegistrationFixture
      with S4LFixture
      with CurrentProfileFixtures

  trait CodeMocks
    extends BusinessRegistrationConnectorMock
      with KeystoreMock
      with SaveForLaterMock
      with WSHTTPMock
      with MockedComponents
      with MockitoSugar

  object MockAuthRedirects {
    def redirectToLogin: Result = Redirect(Call("GET", "/test/login"))

    def redirectToPostSign: Result = Redirect(Call("GET", "/test/post-sign-in"))

    def payeRegElFEUrl = "/prefe"

    def payeRegElFEUri = "/test/"


  }

  def getNameFromAuth(implicit request: Request[AnyContent]): Future[Name]  = Future.successful(Name(Some("Fullname"), None))

}