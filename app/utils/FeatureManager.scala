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

package utils

import javax.inject.Inject

sealed trait FeatureSwitch {
  def name: String
  def value: String
  def enabled: Boolean
}

case class BooleanFeatureSwitch(name: String, enabled: Boolean) extends FeatureSwitch {
  override def value = ""
}

case class ValueSetFeatureSwitch(name: String, setValue: String) extends FeatureSwitch {
  override def enabled = true
  override def value   = setValue
}

class FeatureSwitchManager @Inject extends FeatureManager

trait FeatureManager {

  private[utils] def systemPropertyName(name: String) = s"feature.$name"

  private[utils] def getProperty(name: String): FeatureSwitch = {
    val value = sys.props.get(systemPropertyName(name))

    value match {
      case Some("true")                                            => BooleanFeatureSwitch(name, enabled = true)
      case Some("false")                                           => BooleanFeatureSwitch(name, enabled = false)
      case Some("time-clear")                                      => ValueSetFeatureSwitch(name, "time-clear")
      case Some(date) if date.matches(Validators.datePatternRegex) => ValueSetFeatureSwitch(name, date)
      case _                                                       => BooleanFeatureSwitch(name, enabled = false)
    }
  }

  private[utils] def setProperty(name: String, value: String): FeatureSwitch = {
    sys.props += ((systemPropertyName(name), value))
    getProperty(name)
  }

  def enable(fs: FeatureSwitch): FeatureSwitch  = setProperty(fs.name, "true")
  def disable(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "false")

  def setSystemDate(fs: FeatureSwitch): FeatureSwitch   = setProperty(fs.name, fs.value)
  def clearSystemDate(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "")
}

class PAYEFeatureSwitch @Inject()(val manager: FeatureManager) extends PAYEFeatureSwitches {
  val addressLookupUrl    = "addressService"
  val companyRegistration = "companyRegistration"
  val setSystemDate       = "system-date"
}

trait PAYEFeatureSwitches {

  val addressLookupUrl: String
  val companyRegistration: String
  val setSystemDate: String
  val manager: FeatureManager

  def addressLookupFrontend = manager.getProperty(addressLookupUrl)
  def companyReg            = manager.getProperty(companyRegistration)
  def systemDate            = manager.getProperty(setSystemDate)

  def apply(name: String): Option[FeatureSwitch] = name match {
    case "addressService"       => Some(addressLookupFrontend)
    case "companyRegistration"  => Some(companyReg)
    case "system-date"          => Some(systemDate)
    case _                      => None
  }
}
