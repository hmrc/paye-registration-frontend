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
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class LoggingSpec extends PlaySpec with LogCapturing {

  val ex = new Exception("foobar")

  object TestLogging extends Logging

  "have the logger configured correctly" in {
    TestLogging.logger.logger.getName mustBe "application.utils.TestLogging"
  }

  "when logging" must {

    Seq(None, Some(new Exception("fooBar"))) foreach { optEx =>

      withCaptureOfLoggingFrom(TestLogging.logger) { logs =>
        Seq(
          logMsg(Level.DEBUG, optEx),
          logMsg(Level.INFO, optEx),
          logMsg(Level.WARN, optEx),
          logMsg(Level.ERROR, optEx)
        ) foreach { level =>

          s"at level $level" + optEx.fold("")(s" with exception of " + _) must {

            s"output the correct message and level (prefixing with the class/object name)" in {

              logs.find(_.getLevel == level) match {
                case Some(value) => value.getMessage mustBe s"[TestLogging] $level Message"
                case None => fail(s"Could not find $level message")
              }
            }
          }
        }
      }
    }
  }

  private def logMsg(level: Level, ex: Option[Exception]): Level = {
    import TestLogging.logger
    val msg = s"$level Message"
    level match {
      case Level.DEBUG => ex.fold(logger.debug(msg))(logger.debug(msg, _))
      case Level.INFO =>  ex.fold(logger.info(msg))(logger.info(msg, _))
      case Level.WARN =>  ex.fold(logger.warn(msg))(logger.warn(msg, _))
      case _ =>           ex.fold(logger.error(msg))(logger.error(msg, _))
    }
    level
  }
}
