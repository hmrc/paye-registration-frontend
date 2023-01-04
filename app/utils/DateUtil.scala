/*
 * Copyright 2023 HM Revenue & Customs
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

import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.{TimeZone, ULocale}
import play.api.i18n.Messages

import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.{LocalDate, ZoneId}

trait DateUtil {

  def defaultTimeZone: TimeZone = TimeZone.getTimeZone("Europe/London")

  def toDate(year: String, month: String, day: String): LocalDate = {
    LocalDate.parse(year + "-" + month + "-" + day, DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT))
  }

  def fromDate(date: LocalDate): (String, String, String) = {
    val arrDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).split("-")
    (arrDate(0), arrDate(1), arrDate(2))
  }

  private def toMilliseconds(localDate: LocalDate): Long =
    localDate.atStartOfDay(ZoneId.of(defaultTimeZone.getID)).toInstant.toEpochMilli

  private def dateFormat(pattern: String)(implicit messages: Messages) = createDateFormatForPattern(pattern)

  def formatDate(date: LocalDate, pattern: String)(implicit messages: Messages): String = dateFormat(pattern).format(toMilliseconds(date))

  private def createDateFormatForPattern(pattern: String)(implicit messages: Messages): SimpleDateFormat = {
    val uLocale = new ULocale(messages.lang.code)
    val validLang: Boolean = ULocale.getAvailableLocales.contains(uLocale)
    val locale: ULocale = if (validLang) uLocale else ULocale.getDefault
    val simpleDate = new SimpleDateFormat(pattern, locale)
    simpleDate.setTimeZone(defaultTimeZone)
    simpleDate
  }
}
