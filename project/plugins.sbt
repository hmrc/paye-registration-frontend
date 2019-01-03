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

resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"



addSbtPlugin("uk.gov.hmrc"        % "sbt-artifactory"         % "0.14.0")
addSbtPlugin("uk.gov.hmrc"        %  "sbt-auto-build"         % "1.13.0")
addSbtPlugin("uk.gov.hmrc"        %  "sbt-git-versioning"     % "1.15.0")
addSbtPlugin("uk.gov.hmrc"        %  "sbt-distributables"     % "1.2.0")
addSbtPlugin("com.typesafe.play"  %  "sbt-plugin"             % "2.5.19")
addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin"  % "0.8.0")
addSbtPlugin("org.scoverage"      %  "sbt-scoverage"          % "1.3.5")
