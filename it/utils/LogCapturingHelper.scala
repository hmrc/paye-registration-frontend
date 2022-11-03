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

package utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalatest.Assertions.{fail, succeed}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

trait LogCapturingHelper extends LogCapturing {

  implicit class LogCapturingExtensions(logs: List[ILoggingEvent]) {
    def containsMsg(level: Level, msg: String) =
      logs.find(_.getMessage.contains(msg)) match {
      case Some(log) => if(log.getLevel == level) succeed else fail(
        s"Found a log with the correct message, but the Level was '${log.getLevel}' when expecting '$level'"
      )
      case None => fail(s"Could not find log with message that contains '$msg'. Actual logs recorded: \n - ${logs.map(_.getMessage).mkString("\n - ")}")
    }
  }

}
