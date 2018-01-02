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

package filters

import config.FrontendAppConfig
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HttpVerbs.DELETE
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.frontend.filters.CSRFExceptionsFilter

import scala.concurrent.Future

class PAYECSRFExceptionsFilter(whitelist: Set[String]) extends CSRFExceptionsFilter(whitelist) {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(deleteFilteredHeaders(rh))
  }

  private[filters] def deleteFilteredHeaders(rh: RequestHeader, now: () => DateTime = () => DateTime.now.withZone(DateTimeZone.UTC)) = {
    if (rh.method == DELETE && whitelist.exists(rh.path.matches(_))) {
      rh.copy(headers = rh.headers.add("Csrf-Bypass" -> FrontendAppConfig.csrfBypassValue))
    } else {
      rh.copy(headers = rh.headers.remove("Csrf-Bypass"))
    }
  }
}

object PAYECSRFExceptionsFilter extends PAYECSRFExceptionsFilter(Set.empty)
