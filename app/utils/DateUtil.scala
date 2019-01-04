/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.temporal.ChronoUnit

trait DateUtil {
  def toDate(year: String, month: String, day: String): LocalDate = {
    LocalDate.parse(year + "-" + month + "-" + day, DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT))
  }

  def fromDate(date: LocalDate): (String, String, String) = {
    val arrDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).split("-")
    (arrDate(0), arrDate(1), arrDate(2))
  }
}
