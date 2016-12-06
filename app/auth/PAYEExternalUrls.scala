/*
 * Copyright 2016 HM Revenue & Customs
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

package auth

import controllers.userJourney.routes
import uk.gov.hmrc.play.config.ServicesConfig

object PAYEExternalUrls extends ServicesConfig {

  private[PAYEExternalUrls] val companyAuthHost = getConfString(s"$env.microservice.services.auth.company-auth.url","")
  private[PAYEExternalUrls] val loginCallback = getConfString(s"$env.microservice.services.auth.login-callback.url","")
  private[PAYEExternalUrls] val loginPath = getConfString(s"$env.microservice.services.auth.login_path","")

  val loginURL = s"$companyAuthHost$loginPath"
  val continueURL = s"$loginCallback${routes.SignInOutController.postSignIn()}"

}
