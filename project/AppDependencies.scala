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

  val playSuffix                      =  "-play-30"
  val hmrcMongoVersion                =  "1.9.0"
  val taxYearVersion                  =  "4.0.0"
  val bootstrapVersion                =  "8.6.0"
  val playPartialsVersion             =  "9.1.0"
  val httpCachingVersion              =  "11.2.0"
  val playConditionalMappingVersion   =  "2.0.0"
  val commonsValidatorVersion         =  "1.8.0"
  val scalaTestVersion                =  "3.2.12"
  val playFrontendHmrc                =  "9.10.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  s"bootstrap-frontend$playSuffix"    % bootstrapVersion,
    "uk.gov.hmrc"             %%   "tax-year"                         % taxYearVersion,
    "uk.gov.hmrc"             %%   s"play-partials$playSuffix"        % playPartialsVersion,
    "uk.gov.hmrc"             %%   s"http-caching-client$playSuffix"  % httpCachingVersion,
    "uk.gov.hmrc"             %%   s"play-conditional-form-mapping$playSuffix"    % playConditionalMappingVersion,
    "uk.gov.hmrc.mongo"       %%   s"hmrc-mongo$playSuffix"           % hmrcMongoVersion,
    "commons-validator"       %    "commons-validator"                % commonsValidatorVersion,
    "uk.gov.hmrc"             %%   s"play-frontend-hmrc$playSuffix"   % playFrontendHmrc
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  s"bootstrap-test$playSuffix"        % bootstrapVersion          % Test,
    "org.jsoup"               %   "jsoup"                             % "1.17.2"                  % Test,
    "org.scalatestplus"       %%  "mockito-4-5"                       % s"$scalaTestVersion.0"    % Test,
    "org.scalatestplus.play"  %%  "scalatestplus-play"                % "7.0.1"                   % Test,
    "org.playframework"       %%  "play-test"                         % "3.0.3"                   % Test,
    "com.vladsch.flexmark"    %   "flexmark-all"                      % "0.64.8"                  % Test,
    "org.scalatestplus"       %%  "scalacheck-1-17"                   % "3.2.18.0"                % Test,
    "org.wiremock"            % "wiremock-standalone"                 % "3.5.4"                   % Test,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test$playSuffix"        % hmrcMongoVersion          % Test
  )
}
