/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers.auth

import connectors.KeystoreConnector
import models.external.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, ~}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthHelpers {
  val authConnector: AuthConnector
  val keystoreConnector: KeystoreConnector

  def showAuthorised(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) = {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))

    test(action(request))
  }

  def showUnauthorised(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) = {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InsufficientConfidenceLevel("")))

    test(action(request))
  }

  def showAuthorisedOrg(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) {
    when(authConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(Some(Organisation)))

    test(action(request))
  }

  def showAuthorisedNotOrg(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) {
    when(authConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(Some(Individual)))

    test(action(request))
  }

  def submitAuthorised(action: Action[AnyContent], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))

    test(action(request))
  }

  def submitUnauthorised(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) = {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InsufficientConfidenceLevel("")))

    test(action(request))
  }

  def submitUnauthorisedT[T](action: Action[T], request: Request[T])(test: Future[Result] => Any) = {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InsufficientConfidenceLevel("")))

    test(action(request))
  }

  def showAuthorisedWithCP(action: Action[AnyContent], currentProfile: Option[CurrentProfile], request: Request[AnyContent])(test: Future[Result] => Any) {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))

    when(keystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(currentProfile))

    test(action(request))
  }

  def showAuthorisedWithCpAndAuthResponse(action: Action[AnyContent], currentProfile: Option[CurrentProfile], request: Request[AnyContent])(test: Future[Result] => Any) {
    when(authConnector.authorise[Unit](ArgumentMatchers.eq(ConfidenceLevel.L50), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))

    when(keystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(currentProfile))

    test(action(request))
  }

  def submitAuthorisedWithCP(action: Action[AnyContent], currentProfile: Option[CurrentProfile], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))

    when(keystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(currentProfile))

    test(action(request))
  }


  def submitAuthorisedWithCPAndAudit(action: Action[AnyContent], currentProfile: Option[CurrentProfile], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    when(authConnector.authorise[~[Option[String], Credentials]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(new ~(Some("testId"), Credentials("testProviderId", "testProvideType"))))

    when(keystoreConnector.fetchAndGet[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future(currentProfile))

    test(action(request))
  }
}
