/*
 * Copyright 2018 HM Revenue & Customs
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

import config.WSHttp
import connectors._
import connectors.test.{TestBusinessRegConnector, TestIncorpInfoConnector, TestPAYERegConnector}
import org.scalatest.mockito.MockitoSugar
import services._
import uk.gov.hmrc.auth.core.{AuthConnector => AuthClientConnector}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{FeatureManager, PAYEFeatureSwitch, PAYEFeatureSwitches}

trait MockedComponents {
  self: MockitoSugar =>

  val mockWSHttp           = mock[WSHttp]
  val mockSessionCache     = mock[SessionCache]
  val mockFeatureSwitch    = mock[PAYEFeatureSwitch]
  val mockFeatureSwitches  = mock[PAYEFeatureSwitches]
  val mockFeatureManager   = mock[FeatureManager]

  //Connector mocks
  val mockKeystoreConnector             = mock[KeystoreConnector]
  val mockPayeRegistrationConnector     = mock[PAYERegistrationConnector]
  val mockAuthConnector                 = mock[AuthClientConnector]
  val mockOldAuthConnector              = mock[AuthConnector]
  val mockBusinessRegistrationConnector = mock[BusinessRegistrationConnector]
  val mockCompRegConnector              = mock[CompanyRegistrationConnector]
  val mockS4LConnector                  = mock[S4LConnector]
  val mockPAYERegConnector              = mock[PAYERegistrationConnector]
  val mockAddressLookupConnector        = mock[AddressLookupConnector]
  val mockAuditConnector                = mock[AuditConnector]
  val mockIncorpInfoConnector           = mock[IncorporationInformationConnector]
  val mockTestBusRegConnector           = mock[TestBusinessRegConnector]
  val mockTestIncorpInfoConnector       = mock[TestIncorpInfoConnector]
  val mockTestPayeRegConnector          = mock[TestPAYERegConnector]

  //Service mocks
  val mockIncorpInfoService     = mock[IncorporationInformationService]
  val mockCompanyDetailsService = mock[CompanyDetailsService]
  val mockS4LService            = mock[S4LService]
  val mockPrepopulationService  = mock[PrepopulationService]
  val mockAuditService          = mock[AuditService]
  val mockCurrentProfileService = mock[CurrentProfileService]
  val mockPayeRegService        = mock[PAYERegistrationService]
  val mockAddressLookupService  = mock[AddressLookupService]
  val mockPAYEContactService    = mock[PAYEContactService]
  val mockPrepopService         = mock[PrepopulationService]
  val mockThresholdService      = mock[ThresholdService]
}