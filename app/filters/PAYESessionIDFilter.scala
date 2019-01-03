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

package filters

import akka.event.slf4j.Logger
import akka.stream.Materializer
import controllers.userJourney.{routes => userJourneyRoutes}
import javax.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.Validators

import scala.concurrent.Future

class PAYESessionIDFilterImpl @Inject()(val mat: Materializer) extends PAYESessionIDFilter

trait PAYESessionIDFilter extends Filter {

  private def getHeaderCarrier(request: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    getHeaderCarrier(rh).sessionId match {
      case Some(sessionId) if !sessionId.value.matches(Validators.desSessionRegex) =>
        Logger(getClass.getSimpleName).warn(s"The session Id of ${sessionId.value} doesn't match the DES schema. Redirecting the user to sign in")
        Future.successful(Redirect(userJourneyRoutes.SignInOutController.postSignIn()).withNewSession)
      case _ => f(rh)
    }
  }
}
