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

package config

import com.typesafe.config.Config
import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.config.{ControllerConfig, ControllerConfigs}
import uk.gov.hmrc.play.bootstrap.filters.DefaultLoggingFilter
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.templates.error_template

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MyErrorHandler @Inject()(val messagesApi: MessagesApi,
                               val configuration: Configuration,
                                 error_template: error_template
                              )(implicit appConfig: AppConfig, val ec: ExecutionContext) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String,
                                     heading: String,
                                     message: String
                                    )(implicit request: RequestHeader): Future[Html] =
    Future.successful(error_template(pageTitle, heading, message))
}


class ControllerConfiguration @Inject()(config: Configuration) extends ControllerConfig {
  lazy val controllerConfigs: Config = config.underlying.getConfig("controllers")
}

class LoggingFilter @Inject()(config: ControllerConfigs)
                                 (implicit val materializer: Materializer,
                                  override val ec: ExecutionContext) extends DefaultLoggingFilter(config)