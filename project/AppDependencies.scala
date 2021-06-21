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

  private val bootstrapVersion = "5.4.0"
  private val authClientVersion = "3.0.0-play-26"
  private val playPartialsVersion = "8.1.0-play-26"
  private val httpCachingVersion = "9.5.0-play-26"
  private val playAllowlistVersion = "1.0.0-play-26"
  private val playConditionalMappingVersion = "1.9.0-play-26"
  private val commonsValidatorVersion = "1.6"
  private val reactiveMongoVersion = "8.0.0-play-26"
  private val taxYearVersion = "1.1.0"
  private val govukTemplateVersion = "5.68.0-play-26"
  private val playUiVersion = "9.5.0-play-26"
  private val jsonJodaVersion = "2.9.2"
  private val scalaTestPlusVersion = "3.1.3"
  private val pegdownVersion = "1.6.0"
  private val mockitoCoreVersion = "2.13.0"
  private val jsoupVersion = "1.13.1"
  private val wireMockVersion = "2.26.3"
  private val playMongoTestVersion = "5.0.0-play-26"

  private val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "uk.gov.hmrc" %% "play-allowlist-filter" % playAllowlistVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalMappingVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % reactiveMongoVersion,
    "commons-validator" % "commons-validator" % commonsValidatorVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "com.typesafe.play" %% "play-json-joda" % jsonJodaVersion
  )

  private val unitTestDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % Test,
    "org.pegdown" % "pegdown" % pegdownVersion % Test,
    "org.jsoup" % "jsoup" % jsoupVersion % Test,
    "org.mockito" % "mockito-core" % mockitoCoreVersion % Test
  )

  private val integrationTestDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % IntegrationTest,
    "org.pegdown" % "pegdown" % pegdownVersion % IntegrationTest,
    "org.jsoup" % "jsoup" % jsoupVersion % IntegrationTest,
    "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % IntegrationTest,
    "uk.gov.hmrc" %% "reactivemongo-test" % playMongoTestVersion % IntegrationTest
  )

  def apply(): Seq[ModuleID] = compile ++ unitTestDependencies ++ integrationTestDependencies

}
