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

import play.core.PlayVersion
import sbt._


private object AppDependencies {

  val playSuffix                      =  "-play-28"
  val hmrcMongoVersion                =  "0.73.0"
  val taxYearVersion                  =  "3.0.0"
  val bootstrapVersion                =  "7.7.0"
  val playPartialsVersion             = s"8.3.0$playSuffix"
  val httpCachingVersion              = s"10.0.0$playSuffix"
  val playConditionalMappingVersion   = s"1.11.0$playSuffix"
  val commonsValidatorVersion         =  "1.6"
  val govukTemplateVersion            = s"5.78.0$playSuffix"
  val playUiVersion                   = s"9.11.0$playSuffix"
  val scalaTestVersion                =  "3.2.12"
  val playFrontendHmrc                = s"3.27.0$playSuffix"

  val compile = Seq(
    "uk.gov.hmrc"             %%  s"bootstrap-frontend$playSuffix"    % bootstrapVersion,
    "uk.gov.hmrc"             %%   "tax-year"                         % taxYearVersion,
    "uk.gov.hmrc"             %%   "play-partials"                    % playPartialsVersion,
    "uk.gov.hmrc"             %%   "http-caching-client"              % httpCachingVersion,
    "uk.gov.hmrc"             %%   "play-conditional-form-mapping"    % playConditionalMappingVersion,
    "uk.gov.hmrc.mongo"       %%  s"hmrc-mongo$playSuffix"            % hmrcMongoVersion,
    "commons-validator"       %    "commons-validator"                % commonsValidatorVersion,
    "uk.gov.hmrc"             %%   "govuk-template"                   % govukTemplateVersion,
    "uk.gov.hmrc"             %%   "play-ui"                          % playUiVersion,
    "uk.gov.hmrc"             %%   "play-frontend-hmrc"               % playFrontendHmrc
  )

  val test = Seq(
    "uk.gov.hmrc"             %%  s"bootstrap-test$playSuffix"        % bootstrapVersion          % "test, it",
    "org.jsoup"               %   "jsoup"                             % "1.15.3"                  % "test, it",
    "org.scalatestplus"       %%  "mockito-4-5"                       % s"$scalaTestVersion.0"    % "test, it",
    "org.scalatestplus.play"  %%  "scalatestplus-play"                % "5.1.0"                   % "test, it",
    "com.typesafe.play"       %%  "play-test"                         % PlayVersion.current       % "test, it",
    "com.vladsch.flexmark"    %   "flexmark-all"                      % "0.62.2"                  % "test, it",
    "org.scalatestplus"       %%  "scalacheck-1-16"                   % s"$scalaTestVersion.0"    % "test, it",
    "com.github.tomakehurst"  %   "wiremock-jre8-standalone"          % "2.33.2"                  % "it",
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test$playSuffix"        % hmrcMongoVersion          % "it"
  )

  def apply() = compile ++ test
}