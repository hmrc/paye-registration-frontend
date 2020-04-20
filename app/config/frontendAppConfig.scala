/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models.Address
import models.api.Director
import models.external.OfficerList
import play.api.Mode.Mode
import play.api.Play.{configuration, current}
import play.api.libs.json.{Json, Reads}
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(val environment: Environment,
                                  val runModeConfiguration: Configuration)
  extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val contactFormServiceIdentifier = "SCRS"

  lazy val contactFrontendPartialBaseUrl: String = baseUrl("contact-frontend")

  lazy val analyticsToken: String = loadConfig("google-analytics.token")
  lazy val analyticsHost: String = loadConfig("google-analytics.host")
  lazy val reportAProblemPartialUrl: String = loadConfig("reportAProblemPartialUrl")
  lazy val reportAProblemNonJSUrl: String = loadConfig("reportAProblemNonJSUrl")

  lazy val timeoutInSeconds: String = loadConfig("timeoutInSeconds")
  lazy val timeoutDisplayLength: String = loadConfig("timeoutDisplayLength")

  private def whiteListConfig(key: String): Seq[String] = {
    Some(new String(Base64.getDecoder
      .decode(configuration.getString(key).getOrElse("")), "UTF-8"))
      .map(_.split(",")).getOrElse(Array.empty).toSeq
  }

  private def loadStringConfigBase64(key: String): String = {
    new String(Base64.getDecoder.decode(configuration.getString(key).getOrElse("")), Charset.forName("UTF-8"))
  }

  private def loadJsonConfigBase64[T](key: String)(implicit reads: Reads[T]): T = {
    val json = Json.parse(Base64.getDecoder.decode(configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))))
    json.validate[T].fold(
      errors => throw new Exception(s"Incorrect data for the key: $key and ##  $errors"),
      valid => valid
    )
  }

  lazy val self: String = getConfString("paye-registration-frontend.www.url", "")
  lazy val regIdWhitelist: Seq[String] = whiteListConfig("regIdWhitelist")
  lazy val defaultCompanyName: String = loadStringConfigBase64("defaultCompanyName")
  lazy val defaultCHROAddress: Address = loadJsonConfigBase64[Address]("defaultCHROAddress")
  lazy val defaultSeqDirector: Seq[Director] = loadJsonConfigBase64[Seq[Director]]("defaultSeqDirector")(Director.seqReads)
  lazy val defaultCTStatus: String = loadStringConfigBase64("defaultCTStatus")
  lazy val defaultOfficerList: OfficerList = loadJsonConfigBase64[OfficerList]("defaultOfficerList")(OfficerList.formatModel)
  lazy val uriWhiteList: Set[String] = configuration.getStringSeq("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet
  lazy val csrfBypassValue: String = loadStringConfigBase64("Csrf-Bypass-value")

}

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String

  val contactFrontendPartialBaseUrl: String

  val timeoutInSeconds: String
  val timeoutDisplayLength: String

}

object FrontendAppConfig extends AppConfig with ServicesConfig { //TODO Inject the appconfig class above where it's needed and phase out this object
  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val contactFormServiceIdentifier = "SCRS"

  override lazy val contactFrontendPartialBaseUrl = baseUrl("contact-frontend")

  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val reportAProblemPartialUrl = loadConfig("reportAProblemPartialUrl")
  override lazy val reportAProblemNonJSUrl = loadConfig("reportAProblemNonJSUrl")

  override lazy val timeoutInSeconds: String = loadConfig("timeoutInSeconds")
  override lazy val timeoutDisplayLength: String = loadConfig("timeoutDisplayLength")

  private def whiteListConfig(key: String): Seq[String] = {
    Some(new String(Base64.getDecoder
      .decode(configuration.getString(key).getOrElse("")), "UTF-8"))
      .map(_.split(",")).getOrElse(Array.empty).toSeq
  }

  private def loadStringConfigBase64(key: String): String = {
    new String(Base64.getDecoder.decode(configuration.getString(key).getOrElse("")), Charset.forName("UTF-8"))
  }

  private def loadJsonConfigBase64[T](key: String)(implicit reads: Reads[T]): T = {
    val json = Json.parse(Base64.getDecoder.decode(configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))))
    json.validate[T].fold(
      errors => throw new Exception(s"Incorrect data for the key: $key and ##  $errors"),
      valid => valid
    )
  }

  lazy val regIdWhitelist = whiteListConfig("regIdWhitelist")
  lazy val defaultCompanyName = loadStringConfigBase64("defaultCompanyName")
  lazy val defaultCHROAddress = loadJsonConfigBase64[Address]("defaultCHROAddress")
  lazy val defaultSeqDirector = loadJsonConfigBase64[Seq[Director]]("defaultSeqDirector")(Director.seqReads)
  lazy val defaultCTStatus = loadStringConfigBase64("defaultCTStatus")
  lazy val defaultOfficerList = loadJsonConfigBase64[OfficerList]("defaultOfficerList")(OfficerList.formatModel)
  lazy val uriWhiteList = configuration.getStringSeq("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet
  lazy val csrfBypassValue = loadStringConfigBase64("Csrf-Bypass-value")

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
