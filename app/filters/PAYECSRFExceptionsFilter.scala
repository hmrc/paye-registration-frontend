/*
 * Copyright 2020 HM Revenue & Customs
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

package filters

import akka.stream.Materializer
import config.AppConfig
import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HttpVerbs.{DELETE, POST}
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future

class PAYECSRFExceptionsFilterImpl @Inject()(val mat: Materializer
                                            )(implicit val appConfig: AppConfig) extends PAYECSRFExceptionsFilter

trait PAYECSRFExceptionsFilter extends Filter {

  val appConfig: AppConfig

  lazy val whitelist: Set[String] = appConfig.uriWhiteList

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(deleteFilteredHeaders(rh))
  }

  private[filters] def deleteFilteredHeaders(rh: RequestHeader, now: () => DateTime = () => DateTime.now.withZone(DateTimeZone.UTC)) = {
    if ((rh.method == DELETE || rh.method == POST) && whitelist.exists(uri => rh.path.matches(uri))) {
      rh.copy(headers = rh.headers.add("Csrf-Bypass" -> appConfig.csrfBypassValue))
    } else {
      rh.copy(headers = rh.headers.remove("Csrf-Bypass"))
    }
  }
}
