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

import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.Base64

import javax.inject.{Inject, Singleton}
import models.Address
import models.api.Director
import models.external.OfficerList
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppConfig @Inject()(configuration: Configuration, runMode: RunMode) {

  val servicesConfig = new ServicesConfig(configuration, runMode)

  private def loadConfig(key: String) = servicesConfig.getString(key)

  val contactFormServiceIdentifier = "SCRS"

  lazy val contactFrontendPartialBaseUrl: String = servicesConfig.baseUrl("contact-frontend")
  lazy val contactHost: String = loadConfig("contact-frontend.host")

  lazy val analyticsToken: String = loadConfig("google-analytics.token")
  lazy val analyticsHost: String = loadConfig("google-analytics.host")
  lazy val reportAProblemPartialUrl: String = loadConfig("reportAProblemPartialUrl")
  lazy val reportAProblemNonJSUrl: String = loadConfig("reportAProblemNonJSUrl")

  lazy val timeoutInSeconds: String = loadConfig("timeoutInSeconds")
  lazy val timeoutDisplayLength: String = loadConfig("timeoutDisplayLength")

  private def whiteListConfig(key: String): Seq[String] = {
    Some(new String(Base64.getDecoder
      .decode(configuration.getOptional[String](key).getOrElse("")), "UTF-8"))
      .map(_.split(",")).getOrElse(Array.empty).toSeq
  }

  private def loadStringConfigBase64(key: String): String = {
    new String(Base64.getDecoder.decode(servicesConfig.getString(key)), Charset.forName("UTF-8"))
  }

  private def loadJsonConfigBase64[T](key: String)(implicit reads: Reads[T]): T = {
    val json = Json.parse(Base64.getDecoder.decode(servicesConfig.getString(key)))

    json.validate[T].fold(
      errors => throw new Exception(s"Incorrect data for the key: $key and ##  $errors"),
      valid => valid
    )
  }

  lazy val self: String = servicesConfig.getConfString("paye-registration-frontend.www.url", "")
  lazy val regIdWhitelist: Seq[String] = whiteListConfig("regIdWhitelist")
  lazy val defaultCompanyName: String = loadStringConfigBase64("defaultCompanyName")
  lazy val defaultCHROAddress: Address = loadJsonConfigBase64[Address]("defaultCHROAddress")
  lazy val defaultSeqDirector: Seq[Director] = loadJsonConfigBase64[Seq[Director]]("defaultSeqDirector")(Director.seqReads)
  lazy val defaultCTStatus: String = loadStringConfigBase64("defaultCTStatus")
  lazy val defaultOfficerList: OfficerList = loadJsonConfigBase64[OfficerList]("defaultOfficerList")(OfficerList.formatModel)
  lazy val uriWhiteList: Set[String] = configuration.getOptional[Seq[String]]("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet
  lazy val csrfBypassValue: String = loadStringConfigBase64("Csrf-Bypass-value")

  private def encodeUrl(url: String): String = URLEncoder.encode(url, "UTF-8")

  def accessibilityStatementUrl(pageUri: String) = controllers.routes.AccessibilityStatementController.show(pageUri).url

  def accessibilityReportUrl(userAction: String): String =
    s"$contactHost/contact/accessibility-unauthenticated?service=paye-registration-frontend&userAction=${encodeUrl(userAction)}"

}
