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

import sbt._
import play.core.PlayVersion

object AppDependencies {
  def apply() = MainDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object MainDependencies {
  private val frontendBootstrapVersion        = "4.10.0"
  private val authClientVersion               = "2.20.0-play-25"
  private val playPartialsVersion             = "6.7.0-play-25"
  private val httpCachingVersion              = "8.2.0"
  private val playWhitelistVersion            = "2.0.0"
  private val playConditionalMappingVersion   = "0.2.0"
  private val commonsValidatorVersion         = "1.6"
  private val reactiveMongoVersion            = "6.5.0"
  private val taxYearVersion                  = "0.4.0"

  def apply() = Seq(
    "uk.gov.hmrc"         %% "bootstrap-play-25"             % frontendBootstrapVersion,
    "uk.gov.hmrc"         %% "tax-year"                       % taxYearVersion,
    "uk.gov.hmrc"         %% "auth-client"                    % authClientVersion,
    "uk.gov.hmrc"         %% "play-partials"                  % playPartialsVersion,
    "uk.gov.hmrc"         %% "http-caching-client"            % httpCachingVersion,
    "uk.gov.hmrc"         %% "play-whitelist-filter"          % playWhitelistVersion,
    "uk.gov.hmrc"         %% "play-conditional-form-mapping"  % playConditionalMappingVersion,
    "uk.gov.hmrc"         %% "play-reactivemongo"             % reactiveMongoVersion,
    "commons-validator"   %  "commons-validator"              % commonsValidatorVersion,
    "uk.gov.hmrc"         %% "govuk-template" % "5.26.0-play-25",
    "uk.gov.hmrc"         %% "play-ui" % "7.38.0-play-25"
  )
}

trait TestDependencies {
  val scalaTestPlusVersion     = "2.0.1"
  val hmrcTestVersion          = "2.4.0"
  val scalaTestVersion         = "3.0.4"
  val pegdownVersion           = "1.6.0"
  val mockitoCoreVersion       = "2.13.0"
  val jsoupVersion             = "1.10.3"
  val wireMockVersion          = "2.9.0"
  val playMongoTestVersion     = "3.1.0"

  val scope: Configuration
  val test: Seq[ModuleID]
}

object UnitTestDependencies extends TestDependencies {
  override val scope = Test
  override val test = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
    "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
    "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
    "org.mockito"             %  "mockito-core"       % mockitoCoreVersion        % scope
  )

  def apply() = test
}

object IntegrationTestDependencies extends TestDependencies {
  override val scope = IntegrationTest
  override val test = Seq(
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion      % scope,
    "org.pegdown"             %  "pegdown"            % pegdownVersion            % scope,
    "org.jsoup"               %  "jsoup"              % jsoupVersion              % scope,
    "com.github.tomakehurst"  %  "wiremock"           % wireMockVersion           % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test" % playMongoTestVersion      % scope
  )

  def apply() = test
}

