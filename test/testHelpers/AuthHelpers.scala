/*
 * Copyright 2017 HM Revenue & Customs
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

package testHelpers

import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel, CredentialStrength}

trait AuthHelpers {

  def buildAuthContext: AuthContext = AuthContext(createUserAuthority("testUserId"))

  def createUserAuthority(userId: String, accounts: Accounts = Accounts()): Authority = {
    Authority(userId, accounts, None, None, CredentialStrength.Weak, ConfidenceLevel.L50, None, Some("testEnrolmentUri"), None, "")
  }
}
