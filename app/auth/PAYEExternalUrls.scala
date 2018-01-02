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

package auth

import controllers.userJourney.routes
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}

object PAYEExternalUrls extends RunMode with ServicesConfig {

  private[PAYEExternalUrls] val companyAuthHost = getConfString("auth.company-auth.url","")
  private[PAYEExternalUrls] val loginCallback   = getConfString("auth.login-callback.url","")
  private[PAYEExternalUrls] val loginPath       = getConfString("auth.login_path","")

  val loginURL    = s"$companyAuthHost$loginPath"
  val continueURL = s"$loginCallback${routes.SignInOutController.postSignIn()}"
}
