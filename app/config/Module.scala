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

package config

import com.google.inject.AbstractModule
import connectors._
import connectors.test._
import controllers.test._
import controllers.userJourney._
import filters.{PAYESessionIDFilter, PAYESessionIDFilterImpl}
import services._
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.bootstrap.filters.DefaultLoggingFilter
import utils.{FeatureManager, FeatureSwitchManager, PAYEFeatureSwitch, PAYEFeatureSwitches}

class Module extends AbstractModule {
  override def configure(): Unit = {
    bindHmrcDependencies()
    bindFilters()
    bindUtils()
    bindConnectors()
    bindServices()
    bindOtherControllers()
    bindUserJourneyControllers()
  }

  private def bindFilters(): Unit = {
    bind(classOf[PAYESessionIDFilter]).to(classOf[PAYESessionIDFilterImpl]).asEagerSingleton()
    bind(classOf[DefaultLoggingFilter]).to(classOf[LoggingFilterImpl]).asEagerSingleton()
  }

  private def bindHmrcDependencies(): Unit = {
    bind(classOf[MetricsService]).to(classOf[MetricsServiceImpl]).asEagerSingleton()
    bind(classOf[ShortLivedHttpCaching]).to(classOf[PAYEShortLivedHttpCaching]).asEagerSingleton()
    bind(classOf[ShortLivedCache]).to(classOf[PAYEShortLivedCache]).asEagerSingleton()
    bind(classOf[SessionCache]).to(classOf[PAYESessionCache]).asEagerSingleton()
  }

  private def bindUtils(): Unit = {
    bind(classOf[FeatureManager]).to(classOf[FeatureSwitchManager]).asEagerSingleton()
    bind(classOf[PAYEFeatureSwitches]).to(classOf[PAYEFeatureSwitch]).asEagerSingleton()
  }

  private def bindConnectors(): Unit = {
    bind(classOf[TestBusinessRegConnector]).to(classOf[TestBusinessRegConnectorImpl]).asEagerSingleton()
    bind(classOf[TestIncorpInfoConnector]).to(classOf[TestIncorpInfoConnectorImpl]).asEagerSingleton()
    bind(classOf[TestPAYERegConnector]).to(classOf[TestPAYERegConnectorImpl]).asEagerSingleton()
    bind(classOf[CompanyRegistrationConnector]).to(classOf[CompanyRegistrationConnectorImpl]).asEagerSingleton()
    bind(classOf[IncorporationInformationConnector]).to(classOf[IncorporationInformationConnectorImpl]).asEagerSingleton()
    bind(classOf[KeystoreConnector]).to(classOf[KeystoreConnectorImpl]).asEagerSingleton()
    bind(classOf[PAYERegistrationConnector]).to(classOf[PAYERegistrationConnectorImpl]).asEagerSingleton()
    bind(classOf[S4LConnector]).to(classOf[S4LConnectorImpl]).asEagerSingleton()
  }

  private def bindServices(): Unit = {
    bind(classOf[AuditService]).to(classOf[AuditServiceImpl]).asEagerSingleton()
    bind(classOf[CompanyDetailsService]).to(classOf[CompanyDetailsServiceImpl]).asEagerSingleton()
    bind(classOf[CompletionCapacityService]).to(classOf[CompletionCapacityServiceImpl]).asEagerSingleton()
    bind(classOf[ConfirmationService]).to(classOf[ConfirmationServiceImpl]).asEagerSingleton()
    bind(classOf[CurrentProfileService]).to(classOf[CurrentProfileServiceImpl]).asEagerSingleton()
    bind(classOf[IncorporationInformationService]).to(classOf[IncorporationInformationServiceImpl]).asEagerSingleton()
    bind(classOf[S4LService]).to(classOf[S4LServiceImpl]).asEagerSingleton()
    bind(classOf[EmploymentService]).to(classOf[EmploymentServiceImpl]).asEagerSingleton()
    bind(classOf[EmploymentService]).to(classOf[EmploymentServiceImpl]).asEagerSingleton()
    bind(classOf[NatureOfBusinessService]).to(classOf[NatureOfBusinessServiceImpl]).asEagerSingleton()
    bind(classOf[PAYERegistrationService]).to(classOf[PAYERegistrationServiceImpl]).asEagerSingleton()
    bind(classOf[PrepopulationService]).to(classOf[PrepopulationServiceImpl]).asEagerSingleton()
    bind(classOf[SubmissionService]).to(classOf[SubmissionServiceImpl]).asEagerSingleton()
    bind(classOf[DirectorDetailsService]).to(classOf[DirectorDetailsServiceImpl]).asEagerSingleton()
  }

  private def bindOtherControllers(): Unit = {
    bind(classOf[BusinessProfileController]).to(classOf[BusinessProfileControllerImpl]).asEagerSingleton()
    bind(classOf[FeatureSwitchController]).to(classOf[FeatureSwitchControllerImpl]).asEagerSingleton()
    bind(classOf[TestAddressLookupController]).to(classOf[TestAddressLookupControllerImpl]).asEagerSingleton()
    bind(classOf[TestCacheController]).to(classOf[TestCacheControllerImpl]).asEagerSingleton()
    bind(classOf[TestSetupController]).to(classOf[TestSetupControllerImpl]).asEagerSingleton()
  }

  private def bindUserJourneyControllers(): Unit = {
    bind(classOf[DashboardController]).to(classOf[DashboardControllerImpl]).asEagerSingleton()
    bind(classOf[EligibilityController]).to(classOf[EligibilityControllerImpl]).asEagerSingleton()
  }
}
