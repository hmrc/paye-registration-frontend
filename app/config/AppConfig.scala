/*
 * Copyright 2022 HM Revenue & Customs
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

import models.Address
import models.api.Director
import models.external.OfficerList
import play.api.Configuration
import play.api.i18n.Lang
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{PAYEFeatureSwitch}

import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(configuration: Configuration,
                          featureSwitch: PAYEFeatureSwitch) {

  val servicesConfig = new ServicesConfig(configuration)

  private def loadConfig(key: String) = servicesConfig.getString(key)

  val contactFormServiceIdentifier = "SCRS"

  lazy val contactFrontendPartialBaseUrl: String = servicesConfig.baseUrl("contact-frontend")
  lazy val contactHost: String = loadConfig("contact-frontend.host")

  lazy val reportAProblemPartialUrl: String = loadConfig("reportAProblemPartialUrl")
  lazy val reportAProblemNonJSUrl: String = loadConfig("reportAProblemNonJSUrl")
  lazy val reportAProblemLayout: String = "https://www.tax.service.gov.uk/contact/report-technical-problem?service=SCRS"

  lazy val timeoutInSeconds: Int = servicesConfig.getInt("timeout.timeout")
  lazy val timeoutDisplayLength: Int = servicesConfig.getInt("timeout.countdown")

  private def allowListConfig(key: String): Seq[String] = {
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

  //Questionnaire url
  lazy val compRegFEURL = servicesConfig.getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = servicesConfig.getConfString("company-registration-frontend.www.uri", "")
  lazy val questionnaireLink: String = compRegFEURL + compRegFEURI + "/questionnaire"

  lazy val self: String = servicesConfig.getConfString("paye-registration-frontend.www.url", "")
  lazy val regIdAllowlist: Seq[String] = allowListConfig("regIdAllowlist")
  lazy val defaultCompanyName: String = loadStringConfigBase64("defaultCompanyName")
  lazy val defaultCHROAddress: Address = loadJsonConfigBase64[Address]("defaultCHROAddress")
  lazy val defaultSeqDirector: Seq[Director] = loadJsonConfigBase64[Seq[Director]]("defaultSeqDirector")(Director.seqReads)
  lazy val defaultCTStatus: String = loadStringConfigBase64("defaultCTStatus")
  lazy val defaultOfficerList: OfficerList =
    loadJsonConfigBase64[OfficerList]("defaultOfficerList")(OfficerList.formatModel)

  private def encodeUrl(url: String): String = URLEncoder.encode(url, "UTF-8")

  lazy val accessibilityStatementPath = loadConfig("microservice.services.accessibility-statement.host")
  lazy val accessibilityStatementUrl = s"$accessibilityStatementPath/accessibility-statement/paye-registration"

  def accessibilityReportUrl(userAction: String): String =
    s"$contactHost/contact/accessibility-unauthenticated?service=paye-registration-frontend&userAction=${encodeUrl(userAction)}"

  def languageTranslationEnabled: Boolean = featureSwitch.isWelshEnabled.enabled

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  lazy val taxYearStartDate: String = servicesConfig.getString("tax-year-start-date")

  lazy val currentPayeWeeklyThreshold: Int = servicesConfig.getInt("paye.weekly-threshold")
  lazy val currentPayeMonthlyThreshold: Int = servicesConfig.getInt("paye.monthly-threshold")
  lazy val currentPayeAnnualThreshold: Int = servicesConfig.getInt("paye.annual-threshold")
  lazy val oldPayeWeeklyThreshold: Int = servicesConfig.getInt("paye.old-weekly-threshold")
  lazy val oldPayeMonthlyThreshold: Int = servicesConfig.getInt("paye.old-monthly-threshold")
  lazy val oldPayeAnnualThreshold: Int = servicesConfig.getInt("paye.old-annual-threshold")

  lazy val adminPeriodStart: String = servicesConfig.getString("paye.admin-period-start")
  lazy val adminPeriodEnd: String = servicesConfig.getString("paye.admin-period-end")

}
