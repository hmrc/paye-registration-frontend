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

package config

import com.google.inject.AbstractModule
import connectors._
import connectors.test._
import controllers.errors.{ErrorController, ErrorControllerImpl}
import controllers.feedback.{FeedbackController, FeedbackControllerImpl}
import controllers.internal.{RegistrationController, RegistrationControllerImpl}
import controllers.test._
import controllers.userJourney._
import services._
import uk.gov.hmrc.auth.core.{AuthConnector => AuthClientConnector}
import uk.gov.hmrc.crypto.{ApplicationCrypto, ApplicationCryptoDI}
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.config.inject.{DefaultServicesConfig, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{FeatureManager, FeatureSwitchManager, PAYEFeatureSwitch, PAYEFeatureSwitches}

class Module extends AbstractModule {
  override def configure() = {
    bindHmrcDependencies()
    bindUtils()
    bindConnectors()
    bindServices()
    bindOtherControllers()
    bindUserJourneyControllers()
  }

  private def bindHmrcDependencies(): Unit = {
    bind(classOf[MetricsService]).to(classOf[MetricsServiceImpl]).asEagerSingleton()
    bind(classOf[ServicesConfig]).to(classOf[DefaultServicesConfig]).asEagerSingleton()
    bind(classOf[WSHttp]).to(classOf[WSHttpImpl]).asEagerSingleton()
    bind(classOf[ApplicationCrypto]).to(classOf[ApplicationCryptoDI]).asEagerSingleton()
    bind(classOf[ShortLivedHttpCaching]).to(classOf[PAYEShortLivedHttpCaching]).asEagerSingleton()
    bind(classOf[ShortLivedCache]).to(classOf[PAYEShortLivedCache]).asEagerSingleton()
    bind(classOf[SessionCache]).to(classOf[PAYESessionCache]).asEagerSingleton()
    bind(classOf[AuthConnector]).to(classOf[AuthConnectorImpl]).asEagerSingleton()
    bind(classOf[AuthClientConnector]).to(classOf[AuthClientConnectorImpl]).asEagerSingleton()
  }

  private def bindUtils(): Unit = {
    bind(classOf[FeatureManager]).to(classOf[FeatureSwitchManager]).asEagerSingleton()
    bind(classOf[PAYEFeatureSwitches]).to(classOf[PAYEFeatureSwitch]).asEagerSingleton()
  }

  private def bindConnectors(): Unit = {
    bind(classOf[TestBusinessRegConnector]).to(classOf[TestBusinessRegConnectorImpl]).asEagerSingleton()
    bind(classOf[TestIncorpInfoConnector]).to(classOf[TestIncorpInfoConnectorImpl]).asEagerSingleton()
    bind(classOf[TestPAYERegConnector]).to(classOf[TestPAYERegConnectorImpl]).asEagerSingleton()
    bind(classOf[AddressLookupConnector]).to(classOf[AddressLookupConnectorImpl]).asEagerSingleton()
    bind(classOf[BusinessRegistrationConnector]).to(classOf[BusinessRegistrationConnectorImpl]).asEagerSingleton()
    bind(classOf[CompanyRegistrationConnector]).to(classOf[CompanyRegistrationConnectorImpl]).asEagerSingleton()
    bind(classOf[IncorporationInformationConnector]).to(classOf[IncorporationInformationConnectorImpl]).asEagerSingleton()
    bind(classOf[KeystoreConnector]).to(classOf[KeystoreConnectorImpl]).asEagerSingleton()
    bind(classOf[PAYERegistrationConnector]).to(classOf[PAYERegistrationConnectorImpl]).asEagerSingleton()
    bind(classOf[S4LConnector]).to(classOf[S4LConnectorImpl]).asEagerSingleton()
    bind(classOf[EmailConnector]).to(classOf[EmailConnectorImpl]).asEagerSingleton()
  }

  private def bindServices(): Unit = {
    bind(classOf[AddressLookupService]).to(classOf[AddressLookupServiceImpl]).asEagerSingleton()
    bind(classOf[AuditService]).to(classOf[AuditServiceImpl]).asEagerSingleton()
    bind(classOf[CompanyDetailsService]).to(classOf[CompanyDetailsServiceImpl]).asEagerSingleton()
    bind(classOf[CompletionCapacityService]).to(classOf[CompletionCapacityServiceImpl]).asEagerSingleton()
    bind(classOf[ConfirmationService]).to(classOf[ConfirmationServiceImpl]).asEagerSingleton()
    bind(classOf[CurrentProfileService]).to(classOf[CurrentProfileServiceImpl]).asEagerSingleton()
    bind(classOf[IncorporationInformationService]).to(classOf[IncorporationInformationServiceImpl]).asEagerSingleton()
    bind(classOf[EligibilityService]).to(classOf[EligibilityServiceImpl]).asEagerSingleton()
    bind(classOf[S4LService]).to(classOf[S4LServiceImpl]).asEagerSingleton()
    bind(classOf[EmploymentService]).to(classOf[EmploymentServiceImpl]).asEagerSingleton()
    bind(classOf[NatureOfBusinessService]).to(classOf[NatureOfBusinessServiceImpl]).asEagerSingleton()
    bind(classOf[PAYEContactService]).to(classOf[PAYEContactServiceImpl]).asEagerSingleton()
    bind(classOf[PAYERegistrationService]).to(classOf[PAYERegistrationServiceImpl]).asEagerSingleton()
    bind(classOf[PrepopulationService]).to(classOf[PrepopulationServiceImpl]).asEagerSingleton()
    bind(classOf[SubmissionService]).to(classOf[SubmissionServiceImpl]).asEagerSingleton()
    bind(classOf[SummaryService]).to(classOf[SummaryServiceImpl]).asEagerSingleton()
    bind(classOf[DirectorDetailsService]).to(classOf[DirectorDetailsServiceImpl]).asEagerSingleton()
    bind(classOf[ThresholdService]).to(classOf[ThresholdServiceImpl]).asEagerSingleton()
    bind(classOf[EmailService]).to(classOf[EmailServiceImpl]).asEagerSingleton()
  }

  private def bindOtherControllers(): Unit = {
    bind(classOf[ErrorController]).to(classOf[ErrorControllerImpl]).asEagerSingleton()
    bind(classOf[FeedbackController]).to(classOf[FeedbackControllerImpl]).asEagerSingleton()
    bind(classOf[RegistrationController]).to(classOf[RegistrationControllerImpl]).asEagerSingleton()
    bind(classOf[BusinessProfileController]).to(classOf[BusinessProfileControllerImpl]).asEagerSingleton()
    bind(classOf[FeatureSwitchController]).to(classOf[FeatureSwitchControllerImpl]).asEagerSingleton()
    bind(classOf[TestAddressLookupController]).to(classOf[TestAddressLookupControllerImpl]).asEagerSingleton()
    bind(classOf[TestCacheController]).to(classOf[TestCacheControllerImpl]).asEagerSingleton()
    bind(classOf[TestCCController]).to(classOf[TestCCControllerImpl]).asEagerSingleton()
    bind(classOf[TestCoHoController]).to(classOf[TestCoHoControllerImpl]).asEagerSingleton()
    bind(classOf[TestRegSetupController]).to(classOf[TestRegSetupControllerImpl]).asEagerSingleton()
    bind(classOf[TestSetupController]).to(classOf[TestSetupControllerImpl]).asEagerSingleton()
    bind(classOf[EditSessionController]).to(classOf[EditSessionControllerImpl]).asEagerSingleton()
  }

  private def bindUserJourneyControllers(): Unit = {
    bind(classOf[CompanyDetailsController]).to(classOf[CompanyDetailsControllerImpl]).asEagerSingleton()
    bind(classOf[CompletionCapacityController]).to(classOf[CompletionCapacityControllerImpl]).asEagerSingleton()
    bind(classOf[ConfirmationController]).to(classOf[ConfirmationControllerImpl]).asEagerSingleton()
    bind(classOf[DashboardController]).to(classOf[DashboardControllerImpl]).asEagerSingleton()
    bind(classOf[DirectorDetailsController]).to(classOf[DirectorDetailsControllerImpl]).asEagerSingleton()
    bind(classOf[EligibilityController]).to(classOf[EligibilityControllerImpl]).asEagerSingleton()
    bind(classOf[EmploymentController]).to(classOf[EmploymentControllerImpl]).asEagerSingleton()
    bind(classOf[NatureOfBusinessController]).to(classOf[NatureOfBusinessControllerImpl]).asEagerSingleton()
    bind(classOf[PAYEContactController]).to(classOf[PAYEContactControllerImpl]).asEagerSingleton()
    bind(classOf[PayeStartController]).to(classOf[PayeStartControllerImpl]).asEagerSingleton()
    bind(classOf[SignInOutController]).to(classOf[SignInOutControllerImpl]).asEagerSingleton()
    bind(classOf[SummaryController]).to(classOf[SummaryControllerImpl]).asEagerSingleton()
    bind(classOf[WelcomeController]).to(classOf[WelcomeControllerImpl]).asEagerSingleton()
  }
}
