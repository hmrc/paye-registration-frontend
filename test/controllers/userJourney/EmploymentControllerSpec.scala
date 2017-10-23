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

package controllers.userJourney

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import builders.AuthBuilder
import connectors.PAYERegistrationConnector
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import models.view.{CompanyPension, EmployingStaff, FirstPayment, Subcontractors, Employment => EmploymentView}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import services.EmploymentService
import testHelpers.PAYERegSpec
import utils.DateUtil

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EmploymentControllerSpec extends PAYERegSpec with DateUtil {
  val mockEmploymentService = mock[EmploymentService]
  val mockPayeRegistrationConnector = mock[PAYERegistrationConnector]

  val ineligible = EmploymentView(Some(EmployingStaff(false)), None, Some(Subcontractors(false)), None)
  val validEmploymentViewModel = EmploymentView(Some(EmployingStaff(true)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some(FirstPayment(LocalDate.of(2016, 12, 1))))
  val validEmploymentViewModel2 = EmploymentView(Some(EmployingStaff(false)), Some(CompanyPension(true)), Some(Subcontractors(true)), Some(FirstPayment(LocalDate.of(2016, 12, 1))))
  val nonValidEmploymentViewModel = EmploymentView(None, None, None, None)

  class Setup {
    val controller = new EmploymentCtrl {
      override val authConnector = mockAuthConnector
      override val employmentService = mockEmploymentService
      override val keystoreConnector = mockKeystoreConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val payeRegistrationConnector = mockPayeRegistrationConnector

      override def withCurrentProfile(f: => (CurrentProfile) => Future[Result], payeRegistrationSubmitted: Boolean)(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(CurrentProfile(
          "12345",
          CompanyRegistrationProfile("held", "txId"),
          "ENG",
          payeRegistrationSubmitted = false
        ))
      }
    }
  }

  val fakeRequest = FakeRequest("GET", "/")

  "calling the employingStaff action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.employingStaff()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.employingStaff, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(nonValidEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.employingStaff, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  //TODO
  "calling the submitEmployingStaff action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitEmployingStaff()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Company Pension page when a user enters YES answer" in new Setup {
      when(mockEmploymentService.saveEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel)
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEmployingStaff" -> "true"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/make-pension-payment-next-two-months"
      }
    }

    "redirect to the First Payment page when a user enters NO answer" in new Setup {
      when(mockEmploymentService.saveEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel2)
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEmployingStaff" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/set-paye-scheme-start-date"
      }
    }

    "redirect to the Ineligible page when a user enters NO answer while also having a NO for Subcontractors" in new Setup {
      when(mockEmploymentService.saveEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(ineligible)
      AuthBuilder.submitWithAuthorisedUser(controller.submitEmployingStaff(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "isEmployingStaff" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/ineligible-for-paye"
      }
    }
  }

  "calling the companyPension action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.companyPension()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.companyPension, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(nonValidEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.companyPension, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitCompanyPension action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitCompanyPension()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyPension(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the First Payment page when a user enters NO answer" in new Setup {
      when(mockEmploymentService.saveCompanyPension(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel)
      AuthBuilder.submitWithAuthorisedUser(controller.submitCompanyPension(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "pensionProvided" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/set-paye-scheme-start-date"
      }
    }
  }

  "calling the subcontractors action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.subcontractors()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.subcontractors, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(nonValidEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.subcontractors, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  //TODO
  "calling the submitSubcontractors action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitSubcontractors()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an invalid answer" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitSubcontractors(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody()) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Employment page when a user enters an answer" in new Setup {
      when(mockEmploymentService.saveSubcontractors(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel)
      AuthBuilder.submitWithAuthorisedUser(controller.submitSubcontractors(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "hasContractors" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/employ-staff-next-two-months"
      }
    }

    "redirect to the Ineligible page when a user enters NO answer while also having a NO for Employment" in new Setup {
      when(mockEmploymentService.saveSubcontractors(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(ineligible)
      AuthBuilder.submitWithAuthorisedUser(controller.submitSubcontractors(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "hasContractors" -> "false"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/ineligible-for-paye"
      }
    }
  }

  "calling the firstPayment action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.firstPayment()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 200 for an authorised user with data already saved" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(validEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.firstPayment, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }

    "return 200 for an authorised user with no data" in new Setup {
      when(mockEmploymentService.fetchEmploymentView(ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(Future.successful(nonValidEmploymentViewModel))
      AuthBuilder.showWithAuthorisedUser(controller.firstPayment, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe Status.OK
      }
    }
  }

  "calling the submitFirstPayment action" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submitFirstPayment()(FakeRequest())
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 for an empty year" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "",
        "firstPayMonth" -> "10",
        "firstPayDay" -> "01"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for an empty month" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "2017",
        "firstPayMonth" -> "",
        "firstPayDay" -> "01"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for an empty day" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "2017",
        "firstPayMonth" -> "10",
        "firstPayDay" -> ""
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for an invalid year" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "-3",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "31"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for an invalid day" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "32"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for an invalid month" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "13",
        "firstPayDay" -> "12"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for a date more than 2 months in the future" in new Setup {
      val today = LocalDate.now()
      val futureDate = fromDate(today.plus(4, ChronoUnit.MONTHS))
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> futureDate._1,
        "firstPayMonth" -> futureDate._2,
        "firstPayDay" -> futureDate._3
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 400 for a date before 1900" in new Setup {
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "1899",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "31"
      )) {
        result =>
          status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "redirect to the Summary page when a user enters a valid past date" in new Setup {
      when(mockEmploymentService.saveFirstPayment(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel)
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> "2016",
        "firstPayMonth" -> "12",
        "firstPayDay" -> "01"
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/relationship-to-company"
      }
    }

    "redirect to the Summary page when a user enters a valid future date" in new Setup {
      val today = LocalDate.now()
      val futureDate = fromDate(today.plus(1, ChronoUnit.MONTHS))
      when(mockEmploymentService.saveFirstPayment(ArgumentMatchers.any(), ArgumentMatchers.anyString())(ArgumentMatchers.any())).thenReturn(validEmploymentViewModel)
      AuthBuilder.submitWithAuthorisedUser(controller.submitFirstPayment(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "firstPayYear" -> futureDate._1,
        "firstPayMonth" -> futureDate._2,
        "firstPayDay" -> futureDate._3
      )) {
        result =>
          status(result) shouldBe Status.SEE_OTHER
          result.header.headers("Location") shouldBe "/register-for-paye/relationship-to-company"
      }
    }
  }
}
