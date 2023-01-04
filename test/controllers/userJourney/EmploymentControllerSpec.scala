/*
 * Copyright 2023 HM Revenue & Customs
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

import helpers.{PayeComponentSpec, PayeFakedApp}
import models.view.{EmployingAnyone, EmployingStaff, WillBePaying}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.InternalServerException
import utils.SystemDate
import views.BaseSelectors
import views.html.pages.employmentDetails._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.{global => globalExecutionContext}
import scala.concurrent.Future

class EmploymentControllerSpec extends PayeComponentSpec with PayeFakedApp {

  object Selectors extends BaseSelectors

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockPaidEmployeesPage: paidEmployees = app.injector.instanceOf[paidEmployees]
  lazy val mockWillBePayingPage: willBePaying = app.injector.instanceOf[willBePaying]
  lazy val mockConstructionIndustryPage: constructionIndustry = app.injector.instanceOf[constructionIndustry]
  lazy val mockApplicationDelayedPage: applicationDelayed = app.injector.instanceOf[applicationDelayed]
  lazy val mockSubcontractorsPage: employsSubcontractors = app.injector.instanceOf[employsSubcontractors]
  lazy val mockPaysPensionPage: paysPension = app.injector.instanceOf[paysPension]

  val testController = new EmploymentController(
    mockEmploymentService,
    mockThresholdService,
    mockKeystoreConnector,
    mockAuthConnector,
    mockIncorpInfoService,
    mockIncorpInfoConnector,
    mockPayeRegService,
    mockMcc,
    mockPaidEmployeesPage,
    mockWillBePayingPage,
    mockConstructionIndustryPage,
    mockApplicationDelayedPage,
    mockSubcontractorsPage,
    mockPaysPensionPage)(
    injAppConfig,
    globalExecutionContext
  )

  val emptyView: EmployingStaff = EmployingStaff(None, None, None, None, None)
  val employingAnyoneView: EmployingStaff = EmployingStaff(Some(EmployingAnyone(employing = true, Some(LocalDate.now()))), None, None, None, None)
  val employingAnyoneViewFalse: EmployingStaff = EmployingStaff(Some(EmployingAnyone(employing = false, None)), None, None, None, None)
  val willBePayingView: EmployingStaff = EmployingStaff(None, Some(WillBePaying(willPay = true, Some(true))), None, None, None)
  val willBePayingViewFalse: EmployingStaff = EmployingStaff(None, Some(WillBePaying(willPay = false, None)), None, None, None)
  val willBePayingViewNewTaxYear: EmployingStaff = EmployingStaff(None, Some(WillBePaying(willPay = true, Some(false))), None, None, None)
  val constructionIndustryViewFalse: EmployingStaff = EmployingStaff(None, None, Some(false), None, None)
  val constructionIndustryView: EmployingStaff = EmployingStaff(None, None, Some(true), None, None)
  val pensionsView: EmployingStaff = EmployingStaff(None, None, None, None, Some(true))
  val pensionsViewFalse: EmployingStaff = EmployingStaff(None, None, None, None, Some(false))

  def mockGetThreshold: OngoingStubbing[Map[String, Int]] = when(mockThresholdService.getCurrentThresholds).thenReturn(Map("weekly" -> 120))

  def dynamicViewModel(ea: Boolean = false, wbp: Boolean = false, nty: Boolean = false, cis: Boolean = false, subContractor: Boolean = false): EmployingStaff =
    EmployingStaff(Some(EmployingAnyone(ea, Some(LocalDate.now()))), Some(WillBePaying(wbp, Some(nty))), Some(cis), Some(subContractor), None)

  def dynamicViewModelNoDate(wbp: Boolean = false, nty: Boolean = false, cis: Boolean = false, subContractor: Boolean = false): EmployingStaff =
    EmployingStaff(None, Some(WillBePaying(wbp, Some(nty))), Some(cis), Some(subContractor), None)


  "paidEmployees" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.paidEmployees, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }
    "render the page if an incorp date exists" in {

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentService.fetchEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.paidEmployees, Fixtures.validCurrentProfile, fakeRequest()) {
        result => status(result) mustBe OK
      }
    }

    "redirect to will be paying page if an incorp date doesn't exists" in {

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.paidEmployees, Fixtures.validCurrentProfile, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.employingStaff.url)
      }
    }
  }

  "submitPaidEmployees" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.submitUnauthorised(testController.submitPaidEmployees, fakeRequest("POST")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return a bad request if the form isn't filled in" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "" -> ""
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "return a bad request if the alreadyPays = yes and no date filed in" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "alreadyPays" -> "yes"
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to CIS if the company already pays employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "alreadyPaying" -> "true",
        "earliestDate.Day" -> "12",
        "earliestDate.Month" -> "4",
        "earliestDate.Year" -> LocalDate.now.minusYears(2).getYear.toString
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentService.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
      }
    }

    "redirect to will you be paying page if the company doesn't already pay employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "alreadyPaying" -> "false"
      )

      when(mockIncorpInfoService.getIncorporationDate(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(LocalDate.of(2012, 5, 5))))

      when(mockEmploymentService.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneViewFalse))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPaidEmployees, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.employingStaff.url)
      }
    }
  }

  "employingStaff" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.employingStaff, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }
    "render page" in {
      when(mockEmploymentService.fetchEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      mockGetThreshold

      AuthHelpers.showAuthorisedWithCP(testController.employingStaff, Fixtures.validCurrentProfile, fakeRequest()) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitEmployingStaff" should {
    "redirect if the user is not authorised" in {
      when(mockEmploymentService.saveEmployingAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(employingAnyoneView))

      AuthHelpers.submitUnauthorised(testController.submitEmployingStaff, fakeRequest("POST")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return  a bad request if form is not filled in" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "" -> ""
      )

      mockGetThreshold

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to CIS if user selects no" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "willBePaying" -> "false"
      )

      when(mockEmploymentService.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingViewFalse))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
      }
    }

    "redirect to CIS if user will be paying employees before 6th april" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "willBePaying" -> "true",
        "beforeNewTaxYear" -> "true"
      )

      when(mockEmploymentService.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
      }
    }

    "redirect to Application Delayed Page if user will be paying employees after 6th april" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "willBePaying" -> "true",
        "beforeNewTaxYear" -> "false"
      )

      when(mockEmploymentService.saveWillEmployAnyone(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(willBePayingViewNewTaxYear))

      AuthHelpers.submitAuthorisedWithCP(testController.submitEmployingStaff, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.applicationDelayed.url)
      }
    }
  }


  "applicationDelayed" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.applicationDelayed, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "render the page" in {
      AuthHelpers.showAuthorised(testController.applicationDelayed, fakeRequest()) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitApplicationDelayed" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.submitUnauthorised(testController.submitApplicationDelayed, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "redirect to the CIS page" in {
      val formRequest = fakeRequest().withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorised(testController.submitApplicationDelayed, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.constructionIndustry.url)
      }
    }
  }

  "constructionIndustry" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.constructionIndustry, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "render the page" in {

      when(mockEmploymentService.fetchEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.constructionIndustry, Fixtures.validCurrentProfile, fakeRequest()) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitConstructionIndustry" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.submitUnauthorised(testController.submitConstructionIndustry, fakeRequest("POST")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }
    "return a 500 testing handlePostJourneyConstruction error scenario" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(constructionIndustryView.copy(construction = Some(false))))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          intercept[InternalServerException](await(result))
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to the subcontractors page if yes is selected" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "true"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(true))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(constructionIndustryView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.subcontractors.url)
      }
    }

    "redirect to the pensions page if no is selected and the company already pays employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(ea = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.pensions.url)
      }
    }

    "redirect to the completion capacity page if no is selected and the company will pay employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(wbp = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      }
    }

    "redirect to the don't register page if no is selected and the company will not and has never paid employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel()))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.errors.routes.ErrorController.newIneligible.url)
      }
    }

    "redirect to the completion capacity page if no is selected and the company will pay employees and no incorp date" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModelNoDate(wbp = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      }
    }

    "redirect to the don't register page if no is selected and the company will not and has never paid employees and no incorp date" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )

      when(mockEmploymentService.saveConstructionIndustry(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModelNoDate()))

      AuthHelpers.submitAuthorisedWithCP(testController.submitConstructionIndustry, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.errors.routes.ErrorController.newIneligible.url)
      }
    }
  }

  "subcontractors" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.subcontractors, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "render the page" in {
      System.setProperty("feature.system-date", "2019-05-01T00:00:00")
      val currentTaxYearStart = SystemDate.current.startYear.toString
      val currentTaxYearFinish = SystemDate.current.finishYear.toString

      when(mockEmploymentService.fetchEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.subcontractors, Fixtures.validCurrentProfile, fakeRequest()) {
        result =>
          status(result) mustBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.select(Selectors.p(1)).text() mustBe s"The current tax year is 6 April $currentTaxYearStart to 5 April $currentTaxYearFinish."
      }

      val format: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

      val theTimeNow = format.format(LocalDateTime.now)
      System.setProperty("feature.system-date", theTimeNow)
    }
  }

  "submitSubcontractors" should {
    "redirect if the user is not authorised" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "inConstructionIndustry" -> "false"
      )
      AuthHelpers.submitUnauthorised(testController.submitSubcontractors, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to pension page if the company already pays employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "employsSubcontractors" -> "true"
      )

      when(mockEmploymentService.saveSubcontractors(ArgumentMatchers.eq(true))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(ea = true, cis = true)))

      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.EmploymentController.pensions.url)
      }
    }

    "redirect to the completion capacity page if the company has never paid employees" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "employsSubcontractors" -> "false"
      )

      when(mockEmploymentService.saveSubcontractors(ArgumentMatchers.eq(false))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(dynamicViewModel(wbp = true, cis = true)))


      AuthHelpers.submitAuthorisedWithCP(testController.submitSubcontractors, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      }
    }
  }

  "pensions" should {
    "redirect if the user is not authorised" in {
      AuthHelpers.showUnauthorised(testController.pensions, fakeRequest()) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "render the page" in {

      when(mockEmploymentService.fetchEmployingStaff(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyView))

      AuthHelpers.showAuthorisedWithCP(testController.pensions, Fixtures.validCurrentProfile, fakeRequest()) {
        result => status(result) mustBe OK
      }
    }
  }

  "submitPensions" should {
    "redirect if the user is not authorised" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "payPension" -> "false"
      )
      AuthHelpers.submitUnauthorised(testController.submitPensions, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-in?accountType=organisation&continue_url=http%3A%2F%2Flocalhost%3A9870%2Fregister-for-paye%2Fstart-pay-as-you-earn&origin=paye-registration-frontend")
      }
    }

    "return a bad request if the form is empty" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "" -> ""
      )
      AuthHelpers.submitAuthorisedWithCP(testController.submitPensions, Fixtures.validCurrentProfile, formRequest) {
        result => status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to completion capacity if pays pensions" in {
      val formRequest = fakeRequest("POST").withFormUrlEncodedBody(
        "paysPension" -> "true"
      )

      when(mockEmploymentService.savePensionPayment(ArgumentMatchers.eq(true))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(pensionsView))

      AuthHelpers.submitAuthorisedWithCP(testController.submitPensions, Fixtures.validCurrentProfile, formRequest) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.userJourney.routes.CompletionCapacityController.completionCapacity.url)
      }
    }
  }
}