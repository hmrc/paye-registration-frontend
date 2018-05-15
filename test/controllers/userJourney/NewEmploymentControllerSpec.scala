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

package controllers.userJourney

import java.time.LocalDate

import connectors.KeystoreConnector
import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.{EmployingAnyone, EmployingStaffV2, WillBePaying}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import services._
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class NewEmploymentControllerSpec extends PayeComponentSpec with PayeFakedApp {

  val request = FakeRequest()

  val testController = new NewEmploymentController {
    override val redirectToLogin          = MockAuthRedirects.redirectToLogin
    override val redirectToPostSign       = MockAuthRedirects.redirectToPostSign
    override val thresholdService         = mockThresholdService
    override val incorpInfoService        = mockIncorpInfoService
    override val companyDetailsService    = mockCompanyDetailsService
    override val s4LService               = mockS4LService
    override val authConnector            = mockAuthConnector
    override val keystoreConnector        = mockKeystoreConnector
    override val employmentService        = mockEmploymentServiceV2
    implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    override val incorporationInformationConnector = mockIncorpInfoConnector
    override val payeRegistrationService  = mockPayeRegService
  }

  val emptyView                       = EmployingStaffV2(None, None, None, None, None)
  val employingAnyoneView             = EmployingStaffV2(Some(EmployingAnyone(true, Some(LocalDate.now()))),None,None,None,None)
  val employingAnyoneViewFalse        = EmployingStaffV2(Some(EmployingAnyone(false, None)),None,None,None,None)
  val willBePayingView                = EmployingStaffV2(None, Some(WillBePaying(true, Some(true))), None, None, None)
  val willBePayingViewFalse           = EmployingStaffV2(None, Some(WillBePaying(false, None)), None, None, None)
  val willBePayingViewNewTaxYear      = EmployingStaffV2(None, Some(WillBePaying(true, Some(false))), None, None, None)
  val constructionIndustryViewFalse   = EmployingStaffV2(None, None, Some(false), None, None)
  val constructionIndustryView        = EmployingStaffV2(None, None, Some(true), None, None)
  val pensionsView                    = EmployingStaffV2(None, None, None, None, Some(true))
  val pensionsViewFalse               = EmployingStaffV2(None, None, None, None, Some(false))

  def mockGetThreshold = when(mockThresholdService.getCurrentThresholds).thenReturn(Map("weekly" -> 116))
  def dynamicViewModel(ea: Boolean = false, wbp: Boolean = false, nty: Boolean = false, cis: Boolean = false, subContractor: Boolean = false) =
    EmployingStaffV2(Some(EmployingAnyone(ea, Some(LocalDate.now()))), Some(WillBePaying(wbp, Some(nty))), Some(cis), Some(subContractor), None)

  def dynamicViewModelNoDate(wbp: Boolean = false, nty: Boolean = false, cis: Boolean = false, subContractor: Boolean = false) =
    EmployingStaffV2(None, Some(WillBePaying(wbp, Some(nty))), Some(cis), Some(subContractor), None)


  "paidEmployees" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.paidEmployees, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }
    "render the page if an incorp date exists" in {

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentServiceV2.fetchEmploymentView(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.paidEmployees, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe OK
      }
    }

    "redirect to will be paying page if an incorp date doesn't exists" in {

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.paidEmployees, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.employingStaff().url)
      }
    }
  }

  "submitPaidEmployees" should {
    "redirect if the user is not authorised" in {
      val formRequest = request.withFormUrlEncodedBody(
        "alreadyPaying" -> "false"
      )
      AuthHelpers.submitUnauthorised(testController.submitPaidEmployees, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return a bad request if the form isn't filled in" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "return a bad request if the alreadyPays = yes and no date filed in" in {
      val formRequest = request.withFormUrlEncodedBody(
        "alreadyPays" -> "yes"
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to CIS if the company already pays employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "alreadyPaying" -> "true",
        "earliestDateDay" -> "12",
        "earliestDateMonth" -> "4",
        "earliestDateYear" -> "2017"
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentServiceV2.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)
      }
    }

    "redirect to will you be paying page if the company doesn't already pay employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "alreadyPaying" -> "false"
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentServiceV2.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneViewFalse))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.employingStaff().url)
      }
    }
  }

  "employingStaff" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.employingStaff, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }
    "render page" in {
      when(mockEmploymentServiceV2.fetchEmploymentView(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.employingStaff, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitEmployingStaff" should {
    "redirect if the user is not authorised" in {
      val formRequest = request.withFormUrlEncodedBody(
        "willBePaying" -> "false"
      )

      when(mockEmploymentServiceV2.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneView))

      AuthHelpers.submitUnauthorised(testController.submitEmployingStaff, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return  a bad request if form is not filled in" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to CIS if user selects no" in {
      val formRequest = request.withFormUrlEncodedBody(
        "willBePaying" -> "false"
      )

      when(mockEmploymentServiceV2.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingViewFalse))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)
      }
    }

    "redirect to CIS if user will be paying employees before 6th april" in {
      val formRequest = request.withFormUrlEncodedBody(
        "willBePaying" -> "true",
        "beforeNewTaxYear" -> "true"
      )

      when(mockEmploymentServiceV2.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)
      }
    }

    "redirect to Application Delayed Page if user will be paying employees after 6th april" in {
      val formRequest = request.withFormUrlEncodedBody(
        "willBePaying" -> "true",
        "beforeNewTaxYear" -> "false"
      )

      when(mockEmploymentServiceV2.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingViewNewTaxYear))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.applicationDelayed().url)
      }
    }
  }


  "applicationDelayed" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.applicationDelayed, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "render the page" in {
      AuthHelpers.showAuthorised(testController.applicationDelayed, request) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitApplicationDelayed" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.submitUnauthorised(testController.submitApplicationDelayed, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "redirect to the CIS page" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorised(testController.submitApplicationDelayed, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.constructionIndustry().url)
      }
    }
  }

  "constructionIndustry" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.constructionIndustry, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "render the page" in {

      when(mockEmploymentServiceV2.fetchEmploymentView(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.constructionIndustry, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitConstructionIndustry" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.submitUnauthorised(testController.submitConstructionIndustry, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }
    "return a 500 testing handlePostJourneyConstruction error scenario" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(constructionIndustryView.copy(construction = Some(false))))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe 500
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to the subcontractors page if yes is selected" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "true"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(true))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(constructionIndustryView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.subcontractors().url)
      }
    }

    "redirect to the pensions page if no is selected and the company already pays employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(ea = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.pensions().url)
      }
    }

    "redirect to the completion capacity page if no is selected and the company will pay employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(wbp = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity().url)
      }
    }

    "redirect to the don't register page if no is selected and the company will not and has never paid employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel()))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.errors.routes.ErrorController.newIneligible().url)
      }
    }

    "redirect to the completion capacity page if no is selected and the company will pay employees and no incorp date" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModelNoDate(wbp = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity().url)
      }
    }

    "redirect to the don't register page if no is selected and the company will not and has never paid employees and no incorp date" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentServiceV2.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModelNoDate()))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.errors.routes.ErrorController.newIneligible().url)
      }
    }
  }

  "subcontractors" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.subcontractors, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "render the page" in {

      when(mockEmploymentServiceV2.fetchEmploymentView(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.subcontractors, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitSubcontractors" should {
    "redirect if the user is not authorised" in {
      val formRequest = request.withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )
      AuthHelpers.submitUnauthorised(testController.submitSubcontractors, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to pension page if the company already pays employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "employsSubcontractors" -> "true"
      )

      when(mockEmploymentServiceV2.saveSubcontractors(ArgumentMatchers.eq(true))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(ea = true, cis = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.NewEmploymentController.pensions().url)
      }
    }

    "redirect to the completion capacity page if the company has never paid employees" in {
      val formRequest = request.withFormUrlEncodedBody(
        "employsSubcontractors" -> "false"
      )

      when(mockEmploymentServiceV2.saveSubcontractors(ArgumentMatchers.eq(false))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(wbp = true, cis = true)))


      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity().url)
      }
    }
  }

  "pensions" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.pensions, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "render the page" in {

      when(mockEmploymentServiceV2.fetchEmploymentView(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.pensions, Fixtures.validCurrentProfile, request) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitPensions" should {
    "redirect if the user is not authorised" in {
      val formRequest = request.withFormUrlEncodedBody(
        "payPension" -> "false"
      )
      AuthHelpers.submitUnauthorised(testController.submitPensions, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/test/login")
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = request.withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitPensions, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to completion capacity if pays pensions" in {
      val formRequest = request.withFormUrlEncodedBody(
        "paysPension" -> "true"
      )

      when(mockEmploymentServiceV2.savePensionPayment(ArgumentMatchers.eq(true))(ArgumentMatchers.any(),ArgumentMatchers.any()))
        .thenReturn(Future.successful(pensionsView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPensions, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity().url)
      }
    }
  }
}
