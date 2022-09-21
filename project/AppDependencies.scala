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

import AppDependencies.scalaTestVersion
import play.core.PlayVersion
import sbt._


private object AppDependencies {


  private val hmrcMongoVersion = "0.73.0"
  private val taxYearVersion = "3.0.0"
  private val bootstrapVersion = "5.16.0"
  private val playPartialsVersion = "8.3.0-play-28"
  private val httpCachingVersion = "9.6.0-play-28"
  private val playAllowlistVersion = "1.0.0-play-28"
  private val playConditionalMappingVersion = "1.11.0-play-28"
  private val commonsValidatorVersion = "1.6"
  private val govukTemplateVersion = "5.78.0-play-28"
  private val playUiVersion = "9.11.0-play-28"
  private val scalaTestVersion = "3.2.12"


  val compile = Seq(

    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingVersion,
    "uk.gov.hmrc" %% "play-allowlist-filter" % playAllowlistVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalMappingVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
    "commons-validator" % "commons-validator" % commonsValidatorVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "3.27.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-govuk" % "2.0.0-play-28"
  )

  def defaultTest(scope: String) = Seq(
    "org.jsoup" % "jsoup" % "1.10.3" % scope,
    "org.mockito" % "mockito-core" % "4.1.0" % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % scope,
    "org.scalatestplus" %% "scalacheck-1-16" % s"$scalaTestVersion.0" % scope

  )

  object Test {
    def apply() = defaultTest("test")
  }

  object IntegrationTest {
    def apply() = defaultTest("it") ++ Seq(
      "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2" % "it",
      "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % "it"
    )
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}