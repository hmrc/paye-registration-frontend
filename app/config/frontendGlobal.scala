/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.http.DefaultHttpFilters
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.{Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfig, ControllerConfigs}
import uk.gov.hmrc.play.bootstrap.filters.{DefaultLoggingFilter, FrontendFilters}
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.concurrent.ExecutionContext

class MyErrorHandler @Inject()(val messagesApi: MessagesApi,
                               val configuration: Configuration
                              )(implicit appConfig: AppConfig) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String,
                                     heading: String,
                                     message: String
                                    )(implicit request: Request[_]): Html =
    views.html.error_template(pageTitle, heading, message)
}

class PAYEFilters @Inject()(defaultFilters: FrontendFilters,
                            loggingFilterCustom: DefaultLoggingFilter,
                            sessionIdFilter: PAYESessionIDFilter,
                            csrfeFilterCustom: PAYECSRFExceptionsFilter
                           ) extends DefaultHttpFilters(
  Seq(csrfeFilterCustom) ++ defaultFilters.filters
    :+ loggingFilterCustom
    :+ sessionIdFilter: _*
)

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

class LoggingFilterImpl @Inject()(config: ControllerConfigs)
                                 (implicit val materializer: Materializer,
                                  override val ec: ExecutionContext) extends DefaultLoggingFilter(config)