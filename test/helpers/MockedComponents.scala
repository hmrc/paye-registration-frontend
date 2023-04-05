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

import config.{AppConfig, PAYESessionCache}
import connectors._
import connectors.test.{TestBusinessRegConnector, TestIncorpInfoConnector, TestPAYERegConnector}
import controllers.test.BusinessProfileController
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import repositories.SessionRepository
import services._
import uk.gov.hmrc.auth.core.{AuthConnector => AuthClientConnector}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.language.LanguageUtils
import utils.{FeatureSwitchManager, PAYEFeatureSwitch, TaxYearConfig}

trait MockedComponents {
  self: MockitoSugar =>

  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockHttpClient: HttpClient = mock[HttpClient]
  val mockSessionCache: PAYESessionCache = mock[PAYESessionCache]
  val mockFeatureSwitch: PAYEFeatureSwitch = mock[PAYEFeatureSwitch]
  val mockFeatureManager: FeatureSwitchManager = mock[FeatureSwitchManager]
  val mockLanguageUtils: LanguageUtils = mock[LanguageUtils]
  val mockTaxYearConfig: TaxYearConfig = mock[TaxYearConfig]

  //Connector mocks
  val mockKeystoreConnector: KeystoreConnector = mock[KeystoreConnector]
  val mockPayeRegistrationConnector: PAYERegistrationConnector = mock[PAYERegistrationConnector]
  val mockAuthConnector: AuthClientConnector = mock[AuthClientConnector]
  val mockOldAuthConnector: AuthClientConnector = mock[AuthClientConnector]
  val mockBusinessRegistrationConnector: BusinessRegistrationConnector = mock[BusinessRegistrationConnector]
  val mockCompRegConnector: CompanyRegistrationConnector = mock[CompanyRegistrationConnector]
  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val mockS4LConnector: S4LConnector = mock[S4LConnector]
  val mockPAYERegConnector: PAYERegistrationConnector = mock[PAYERegistrationConnector]
  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockIncorpInfoConnector: IncorporationInformationConnector = mock[IncorporationInformationConnector]
  val mockTestBusRegConnector: TestBusinessRegConnector = mock[TestBusinessRegConnector]
  val mockTestIncorpInfoConnector: TestIncorpInfoConnector = mock[TestIncorpInfoConnector]
  val mockTestPayeRegConnector: TestPAYERegConnector = mock[TestPAYERegConnector]
  val mockConfirmationService: ConfirmationService = mock[ConfirmationService]
  val mockBusinessProfileController: BusinessProfileController = mock[BusinessProfileController]

  //Service mocks
  val mockIncorpInfoService: IncorporationInformationService = mock[IncorporationInformationService]
  val mockCompanyDetailsService: CompanyDetailsService = mock[CompanyDetailsService]
  val mockS4LService: S4LService = mock[S4LService]
  val mockEmailService: EmailService = mock[EmailService]
  val mockPrepopulationService: PrepopulationService = mock[PrepopulationService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockCurrentProfileService: CurrentProfileService = mock[CurrentProfileService]
  val mockPayeRegService: PAYERegistrationService = mock[PAYERegistrationService]
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]
  val mockPAYEContactService: PAYEContactService = mock[PAYEContactService]
  val mockPrepopService: PrepopulationService = mock[PrepopulationService]
  val mockThresholdService: ThresholdService = mock[ThresholdService]
  val mockEmploymentService: EmploymentService = mock[EmploymentService]
  val mockAddressLookupConfigBuilderServiceMock: AddressLookupConfigBuilderService = mock[AddressLookupConfigBuilderService]
  val mockMetricService: MetricsService = mock[MetricsService]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockConfiguration: Configuration = mock[Configuration]
  val mockEnvironment: Environment = mock[Environment]
}
