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

package jobs

import akka.actor.{ActorSystem, Cancellable}
import common.Logging

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

sealed trait JobCompletionStatus
case object JobComplete extends JobCompletionStatus
case object JobFailed extends JobCompletionStatus
private case object JobDisabled extends JobCompletionStatus

trait ScheduledJob extends Logging {
  val actorSystem: ActorSystem
  implicit val executionContext: ExecutionContext

  val jobName: String
  val enabled: Boolean
  val interval: Long
  val timeUnit: String

  def scheduledJob: Future[JobCompletionStatus]

  private def createInterval(interval: Long, timeUnit: String): FiniteDuration = timeUnit.toUpperCase match {
    case "DAY"    => interval.day
    case "HOUR"   => interval.hour
    case "MINUTE" => interval.minute
    case "SECOND" => interval.second
  }

  private def jobRunner(f: => Future[JobCompletionStatus]): Cancellable =
    actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = createInterval(interval, timeUnit))(f)

  if(enabled) {
    jobRunner(scheduledJob)
  } else {
    logger.info(s"job $jobName is currently disabled")
    Future(JobDisabled)
  }
}
