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

import java.time.LocalDateTime

import javax.inject.Inject
import org.joda.time.format.ISODateTimeFormat

sealed trait FeatureSwitch {
  def name: String
  def value: String
  def enabled: Boolean
}

trait TimedFeatureSwitch extends FeatureSwitch {

  def start: Option[LocalDateTime]
  def end: Option[LocalDateTime]
  def target: LocalDateTime

  override def enabled: Boolean = (start, end) match {
    case (Some(s), Some(e)) => !target.isBefore(s) && !target.isAfter(e)
    case (None, Some(e))    => !target.isAfter(e)
    case (Some(s), None)    => !target.isBefore(s)
    case (None, None)       => false
  }
}

case class EnabledTimedFeatureSwitch(name: String, start: Option[LocalDateTime], end: Option[LocalDateTime], target: LocalDateTime) extends TimedFeatureSwitch {
  override def value = ""
}
case class DisabledTimedFeatureSwitch(name: String, start: Option[LocalDateTime], end: Option[LocalDateTime], target: LocalDateTime) extends TimedFeatureSwitch {
  override def enabled: Boolean = !super.enabled
  override def value            = ""
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

  val DisabledIntervalExtractor = """!(\S+)_(\S+)""".r
  val EnabledIntervalExtractor  = """(\S+)_(\S+)""".r
  val UNSPECIFIED               = "X"
  val dateFormat                = ISODateTimeFormat.dateTimeNoMillis()

  private[utils] def toDate(text: String) : Option[LocalDateTime] = {
    text match {
      case UNSPECIFIED => None
      case _           => Some(LocalDateTime.parse(text))
    }
  }

  private[utils] def systemPropertyName(name: String) = s"feature.$name"

  private[utils] def getProperty(name: String): FeatureSwitch = {
    val value = sys.props.get(systemPropertyName(name))

    value match {
      case Some("true")                                            => BooleanFeatureSwitch(name, enabled = true)
      case Some(DisabledIntervalExtractor(start, end))             => DisabledTimedFeatureSwitch(name, toDate(start), toDate(end), SystemDate.getSystemDate)
      case Some(EnabledIntervalExtractor(start, end))              => EnabledTimedFeatureSwitch(name, toDate(start), toDate(end), SystemDate.getSystemDate)
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

  def apply(name: String, enabled: Boolean = false): FeatureSwitch = getProperty(name)
  def unapply(fs: FeatureSwitch): Option[(String, Boolean)]        = Some(fs.name -> fs.enabled)
}

class PAYEFeatureSwitch @Inject()(val manager: FeatureManager) extends PAYEFeatureSwitches {
  val addressLookupUrl    = "addressService"
  val companyRegistration = "companyRegistration"
  val setSystemDate       = "system-date"
  val publicBetaFeature   = "publicBeta"
}

trait PAYEFeatureSwitches {

  protected val addressLookupUrl: String
  protected val companyRegistration: String
  protected val setSystemDate: String
  protected val manager: FeatureManager
  protected val publicBetaFeature: String

  def addressLookupFrontend = manager.getProperty(addressLookupUrl)
  def companyReg            = manager.getProperty(companyRegistration)
  def systemDate            = manager.getProperty(setSystemDate)
  def publicBeta            = manager.getProperty(publicBetaFeature)

  def apply(name: String): Option[FeatureSwitch] = name match {
    case "addressService"       => Some(addressLookupFrontend)
    case "companyRegistration"  => Some(companyReg)
    case "system-date"          => Some(systemDate)
    case "publicBeta"           => Some(publicBeta)
    case _                      => None
  }

  def all: Seq[FeatureSwitch] = {
    Seq(addressLookupFrontend, companyReg, systemDate, publicBeta)
  }
}
