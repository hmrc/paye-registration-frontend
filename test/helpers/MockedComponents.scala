/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors._
import connectors.test.{TestBusinessRegConnector, TestIncorpInfoConnector, TestPAYERegConnector}
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import repositories.SessionRepository
import services._
import uk.gov.hmrc.auth.core.{AuthConnector => AuthClientConnector}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{FeatureManager, PAYEFeatureSwitch, PAYEFeatureSwitches}

trait MockedComponents {
  self: MockitoSugar =>

  val mockHttpClient = mock[HttpClient]
  val mockSessionCache = mock[SessionCache]
  val mockFeatureSwitch = mock[PAYEFeatureSwitch]
  val mockFeatureSwitches = mock[PAYEFeatureSwitches]
  val mockFeatureManager = mock[FeatureManager]
  val mockLanguageUtils = mock[LanguageUtils]

  //Connector mocks
  val mockKeystoreConnector = mock[KeystoreConnector]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]
  val mockAuthConnector = mock[AuthClientConnector]
  val mockOldAuthConnector = mock[AuthClientConnector]
  val mockBusinessRegistrationConnector = mock[BusinessRegistrationConnector]
  val mockCompRegConnector = mock[CompanyRegistrationConnector]
  val mockEmailConnector = mock[EmailConnector]
  val mockS4LConnector = mock[S4LConnector]
  val mockPAYERegConnector = mock[PAYERegistrationConnector]
  val mockAddressLookupConnector = mock[AddressLookupConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockIncorpInfoConnector = mock[IncorporationInformationConnector]
  val mockTestBusRegConnector = mock[TestBusinessRegConnector]
  val mockTestIncorpInfoConnector = mock[TestIncorpInfoConnector]
  val mockTestPayeRegConnector = mock[TestPAYERegConnector]
  val mockConfirmationService = mock[ConfirmationService]

  //Service mocks
  val mockIncorpInfoService = mock[IncorporationInformationService]
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockS4LService = mock[S4LService]
  val mockEmailService = mock[EmailService]
  val mockPrepopulationService = mock[PrepopulationService]
  val mockAuditService = mock[AuditService]
  val mockCurrentProfileService = mock[CurrentProfileService]
  val mockPayeRegService = mock[PAYERegistrationService]
  val mockAddressLookupService = mock[AddressLookupService]
  val mockPAYEContactService = mock[PAYEContactService]
  val mockPrepopService = mock[PrepopulationService]
  val mockThresholdService = mock[ThresholdService]
  val mockEmploymentService = mock[EmploymentService]
  val mockAddressLookupConfigBuilderServiceMock = mock[AddressLookupConfigBuilderService]

  val mockSessionRepository = mock[SessionRepository]
  val mockConfiguration = mock[Configuration]
  val mockEnvironment = mock[Environment]
}
