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

import java.nio.charset.Charset
import java.util.Base64

import models.Address
import models.api.Director
import models.external.OfficerList
import play.api.Play.{configuration, current}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String

  val contactFrontendPartialBaseUrl : String

  val timeoutInSeconds: String
}

object FrontendAppConfig extends AppConfig with ServicesConfig {
  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val contactFormServiceIdentifier = "SCRS"

  override lazy val contactFrontendPartialBaseUrl = baseUrl("contact-frontend")

  override lazy val analyticsToken            = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost             = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl  = s"/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl    = s"/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  override lazy val timeoutInSeconds: String = loadConfig("timeoutInSeconds")

  private def whiteListConfig(key : String) : Seq[String] = {
    Some(new String(Base64.getDecoder
      .decode(configuration.getString(key).getOrElse("")), "UTF-8"))
      .map(_.split(",")).getOrElse(Array.empty).toSeq
  }

  lazy val whitelist          = whiteListConfig("whitelist")
  lazy val whitelistExcluded  = whiteListConfig("whitelist-excluded")

  private def loadStringConfigBase64(key : String) : String = {
    new String(Base64.getDecoder.decode(configuration.getString(key).getOrElse("")), Charset.forName("UTF-8"))
  }

  private def loadJsonConfigBase64[T](key: String)(implicit reads: Reads[T]): T = {
    val json = Json.parse(Base64.getDecoder.decode(configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))))
    json.validate[T].fold(
      errors => throw new Exception(s"Incorrect data for the key: $key and ##  $errors"),
      valid  => valid
    )
  }

  lazy val regIdWhitelist     = whiteListConfig("regIdWhitelist")
  lazy val defaultCompanyName = loadStringConfigBase64("defaultCompanyName")
  lazy val defaultCHROAddress = loadJsonConfigBase64[Address]("defaultCHROAddress")
  lazy val defaultSeqDirector = loadJsonConfigBase64[Seq[Director]]("defaultSeqDirector")(Director.seqReads)
  lazy val defaultCTStatus    = loadStringConfigBase64("defaultCTStatus")
  lazy val defaultOfficerList = loadJsonConfigBase64[OfficerList]("defaultOfficerList")(OfficerList.formatModel)
  lazy val uriWhiteList       = configuration.getStringSeq("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet
  lazy val csrfBypassValue    = loadStringConfigBase64("Csrf-Bypass-value")
}
