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

package builders

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

import scala.concurrent.Future

object AuthBuilder extends AuthBuilder {}

trait AuthBuilder {

  def mockAuthorisedUser(userId: String, mockAuthConnector : AuthConnector, accounts: Accounts = Accounts()) {
    when(mockAuthConnector.currentAuthority(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(Some(createUserAuthority(userId, accounts)))
    }
  }

  def showWithUnauthorisedUser(action: Action[AnyContent])(test: Future[Result] => Any) {
    val result = action.apply()(FakeRequest())
    test(result)
  }

  def showWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector)(test: Future[Result] => Any) {
    val userId = "testUserId"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = action.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnauthorisedUser(action: Action[AnyContent], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, ""))
    test(result)
  }

  def submitWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector, request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = "testUserId"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def createTestUser: AuthContext = {
    AuthContext.apply(createUserAuthority("testUserId"))
  }

  private[builders] def createUserAuthority(userId: String, accounts: Accounts = Accounts()): Authority = {
    Authority(userId, accounts, None, None, CredentialStrength.Weak, ConfidenceLevel.L50, None, Some("testEnrolmentUri"), None, "")
  }
}
