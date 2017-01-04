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

package mocks

import mocks.internal._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait PAYEMocks
  extends SaveForLaterMock
    with KeystoreMock
    with WSHTTPMock
    with BusinessRegistrationConnectorMock {

  this: MockitoSugar =>
    lazy val mockAuthConnector = mock[AuthConnector]
    lazy val mockSessionCache = mock[SessionCache]
    lazy val mockAudit = mock[Audit]

}
