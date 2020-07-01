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

object AppDependencies {
  def apply() = MainDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object MainDependencies {
  private val bootstrapVersion = "1.11.0"
  private val authClientVersion = "3.0.0-play-26"
  private val playPartialsVersion = "6.11.0-play-26"
  private val httpCachingVersion = "9.1.0-play-26"
  private val playWhitelistVersion = "3.4.0-play-26"
  private val playConditionalMappingVersion = "1.2.0-play-26"
  private val commonsValidatorVersion = "1.6"
  private val reactiveMongoVersion = "7.27.0-play-26"
  private val taxYearVersion = "1.1.0"
  private val govukTemplateVersion = "5.55.0-play-26"
  private val playUiVersion = "8.11.0-play-26"
  private val jsonJodaVersion = "2.7.4"

  def apply() = Seq(
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter" % playWhitelistVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalMappingVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % reactiveMongoVersion,
    "commons-validator" % "commons-validator" % commonsValidatorVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "com.typesafe.play" %% "play-json-joda" % jsonJodaVersion
  )
}

trait TestDependencies {
  val scalaTestPlusVersion = "3.1.3"
  val scalaTestVersion = "3.0.8"
  val pegdownVersion = "1.6.0"
  val mockitoCoreVersion = "2.13.0"
  val jsoupVersion = "1.13.1"
  val wireMockVersion = "2.26.3"
  val playMongoTestVersion = "4.19.0-play-26"

  val scope: Configuration
  val test: Seq[ModuleID]
}

object UnitTestDependencies extends TestDependencies {
  override val scope = Test
  override val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % jsoupVersion % scope,
    "org.mockito" % "mockito-core" % mockitoCoreVersion % scope,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.11.0" % Test classifier "tests"
  )

  def apply() = test
}

object IntegrationTestDependencies extends TestDependencies {
  override val scope = IntegrationTest
  override val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % jsoupVersion % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % playMongoTestVersion % scope
  )

  def apply() = test
}
