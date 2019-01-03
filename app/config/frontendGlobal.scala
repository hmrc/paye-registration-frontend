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

package config

import akka.stream.Materializer
import com.typesafe.config.Config
import filters.{PAYECSRFExceptionsFilter, PAYESessionIDFilter}
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import play.api.http.DefaultHttpFilters
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.bootstrap.filters.{FrontendFilters, LoggingFilter}
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.play.config.ControllerConfig


class MyErrorHandler @Inject()(
                                val messagesApi: MessagesApi, val configuration: Configuration) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]) =  views.html.error_template(pageTitle, heading, message)
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}
class PAYEFilters @Inject()(defaultFilters: FrontendFilters,
                            whitelistFilterCustom: WhiteListFilter,
                            loggingFilterCustom: LoggingFilterCustom,
                            sessionIdFilter: PAYESessionIDFilter,
                            csrfeFilterCustom: PAYECSRFExceptionsFilter) extends DefaultHttpFilters(
  Seq(csrfeFilterCustom) ++ defaultFilters.frontendFilters
    :+ whitelistFilterCustom
    :+ loggingFilterCustom
    :+ sessionIdFilter: _*)

class  LoggingFilterImpl @Inject()(val mat: Materializer)  extends LoggingFilterCustom {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}
trait LoggingFilterCustom extends LoggingFilter