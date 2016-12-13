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

package auth.test

import controllers.test.routes
import uk.gov.hmrc.play.config.{ServicesConfig, RunMode}

object TestPAYEExternalUrls extends RunMode with ServicesConfig {

  private[TestPAYEExternalUrls] val companyAuthHost = getConfString("auth.company-auth.url","")
  private[TestPAYEExternalUrls] val loginCallback = getConfString("auth.login-callback.url","")
  private[TestPAYEExternalUrls] val loginPath = getConfString("auth.login_path","")

  val loginURL = s"$companyAuthHost$loginPath"
  val continueURL = s"$loginCallback${routes.CurrentProfileController.currentProfileSetup()}"

}
