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

package services

import helpers.PayeComponentSpec
import models.api.{Employing, Employment}
import models.view.{EmployingAnyone, EmployingStaff, WillBePaying}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class EmploymentServiceSpec extends PayeComponentSpec {

  def testService(date: LocalDate = LocalDate.of(2018, 1, 1)): EmploymentService = new EmploymentService {
    override def now: LocalDate = date

    override val s4LService = mockS4LService
    override val payeRegConnector = mockPayeRegistrationConnector
    override val iiService = mockIncorpInfoService
    override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  }

  val anotherDateEntered = LocalDate.of(2016, 1, 1)
  val newTaxYearDateInRange = LocalDate.of(2018, 4, 4)
  val alreadyEmployingViewModel = EmployingStaff(Some(EmployingAnyone(true, Some(anotherDateEntered))), None, Some(true), Some(true), Some(true))
  val alreadyEmployingApiModel = Employment(Employing.alreadyEmploying, anotherDateEntered, true, true, Some(true))
  val notEmployingViewModel = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None)), Some(false), None, None)
  val notEmployingApiModel = Employment(Employing.notEmploying, testService().now, false, false, None)
  val notEmployingApiModelPreIncorpSubs = Employment(Employing.notEmploying, testService().now, true, true, None)
  val notEmployingViewModelPreIncorp = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None)), Some(false), None, None)
  val notEmployingViewModelPreIncorpSubs = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None)), Some(true), Some(true), None)
  val willEmployThisYearViewModel = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(true))), Some(false), None, None)
  val willEmployThisYearViewModelCTY = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(true, None)), Some(false), None, None)
  val willEmployThisYearViewModelPreIncorp = EmployingStaff(None, Some(WillBePaying(true, Some(true))), Some(false), None, None)
  val willEmployThisYearApiModel = Employment(Employing.willEmployThisYear, testService().now, false, false, None)
  val willEmployNextYearViewModel = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(true, Some(false))), Some(true), Some(false), None)
  val willEmployNextYearViewModelCTY = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(true, None)), Some(true), Some(false), None)
  val willEmployNextYearViewModelPreIncorp = EmployingStaff(None, Some(WillBePaying(true, Some(false))), Some(true), Some(false), None)
  val willEmployNextYearApiModel = Employment(Employing.willEmployNextYear, LocalDate.of(2018, 4, 6), true, false, None)
  val defaultPensionViewModelPreIncorp = EmployingStaff(None, Some(WillBePaying(true, Some(false))), Some(true), Some(false), Some(true))
  val defaultPensionApiModelPreIncorp = Employment(Employing.willEmployNextYear, LocalDate.of(2018, 4, 6), true, false, None)
  val defaultPensionViewModel = EmployingStaff(Some(EmployingAnyone(false, None)), Some(WillBePaying(false, None)), Some(false), None, Some(true))
  val defaultPensionApiModel = Employment(Employing.notEmploying, testService().now, false, false, None)


  "calling viewToAPIV2 with EmployingStaffV2" should {
    "return corresponding converted Employment API Model with Employing = alreadyEmploying with contractors None" in {
      testService().viewToApi(
        EmployingStaff(Some(EmployingAnyone(true, Some(LocalDate.of(2017, 12, 12)))), None, Some(false), None, Some(true))
      ) mustBe Right(Employment(Employing.alreadyEmploying, LocalDate.of(2017, 12, 12), false, false, Some(true)))
    }
    "return corresponding converted Employment API Model with Employing = alreadyEmploying" in {
      testService().viewToApi(alreadyEmployingViewModel) mustBe Right(alreadyEmployingApiModel)
    }

    "return corresponding converted Employment API model with Employing = notEmploying and default subcontractors to false and setting pension to None" in {
      testService().viewToApi(notEmployingViewModel) mustBe Right(notEmployingApiModel)
    }

    "return corresponding converted Employment API model with Employing = willEmployThisYear" in {
      testService().viewToApi(willEmployThisYearViewModel) mustBe Right(willEmployThisYearApiModel)
    }

    "return corresponding converted Employment API model with Employing = willEmployNextYear and set payment date as 6 4 of this year" in {
      testService().viewToApi(willEmployNextYearViewModel) mustBe Right(willEmployNextYearApiModel)
    }
    "return corresponding converted Employment API model with employing = notEmploying and default pension to None" in {
      testService().viewToApi(defaultPensionViewModel) mustBe Right(defaultPensionApiModel)
    }

    "return corresponding converted Employment API model with Employing = notEmploying user is pre incorp and default subcontractors to false" in {
      testService().viewToApi(notEmployingViewModelPreIncorp) mustBe Right(notEmployingApiModel)
    }

    "return corresponding converted Employment API model with Employing = notEmploying and dont default subcontractors user is pre incorp" in {
      testService().viewToApi(notEmployingViewModelPreIncorp) mustBe Right(notEmployingApiModel)
    }

    "return corresponding converted Employment API model with Employing = willEmployThisYear user is pre incorp" in {
      testService().viewToApi(willEmployThisYearViewModelPreIncorp) mustBe Right(willEmployThisYearApiModel)
    }

    "return corresponding converted Employment API model with Employing = willEmployNextYear and set payment date as 6 4 of this year user is pre incorp" in {
      testService().viewToApi(willEmployNextYearViewModel) mustBe Right(willEmployNextYearApiModel)
    }
    "return corresponding Employment API model when pension is provided user is pre incorp so pension is defaulted to None" in {
      testService().viewToApi(defaultPensionViewModelPreIncorp) mustBe Right(defaultPensionApiModelPreIncorp)
    }

    "return viewModel if model is not complete" in {
      val viewModel = EmployingStaff(Some(EmployingAnyone(false, None)), None, None, None, Some(true))
      testService().viewToApi(viewModel) mustBe Left(viewModel)
    }
    "return viewModel if model is not complete pre incorp" in {
      val viewModel = EmployingStaff(None, Some(WillBePaying(true, Some(false))), None, Some(true), None)
      testService().viewToApi(viewModel) mustBe Left(viewModel)
    }
  }

  "apiToView" should {
    val incorpDate = Some(LocalDate.now)
    "return corresponding converted EmploymentStaff View Model with Employing = alreadyEmploying" in {
      testService().apiToView(alreadyEmployingApiModel, incorpDate) mustBe alreadyEmployingViewModel
    }
    "return corresponding converted EmploymentStaff View Model with Employing = notEmploying and default subcontractors to false" in {
      testService().apiToView(notEmployingApiModel, incorpDate) mustBe notEmployingViewModel
    }
    "return corresponding converted EmployingStaff View model with Employing = willEmployThisYear" in {
      testService().apiToView(willEmployThisYearApiModel, incorpDate) mustBe willEmployThisYearViewModelCTY
    }
    "return corresponding converted EmployingStaff View model with Employing = willEmployThisYear in before next tax year range" in {
      testService(newTaxYearDateInRange).apiToView(willEmployThisYearApiModel, incorpDate) mustBe willEmployThisYearViewModel
    }
    "return corresponding converted EmployingStaff View model with Employing = willEmployNextYear and set payment date as 6 4 of this year" in {
      testService().apiToView(willEmployNextYearApiModel, incorpDate) mustBe willEmployNextYearViewModelCTY
    }
    "return corresponding converted EmployingStaff View model with Employing = willEmployNextYear and set payment date as 6 4 of this year in before next tax year range" in {
      testService(newTaxYearDateInRange).apiToView(willEmployNextYearApiModel, incorpDate) mustBe willEmployNextYearViewModel
    }
    "return corresponding EmploymentStaff View model with Employing = notEmploying and default subcontractors to false (AND default Employing to None) for pre incorp" in {
      testService().apiToView(notEmployingApiModel, None) mustBe notEmployingViewModel.copy(employingAnyone = None, companyPension = None)
    }
    "return corresponding EmploymentStaff View model with Employing = willEmployThisYear and pension is None (AND default Employing to None) for pre incorp" in {
      testService().apiToView(willEmployThisYearApiModel, None) mustBe willEmployThisYearViewModelCTY.copy(employingAnyone = None, companyPension = None)
    }
    "return corresponding EmploymentStaff View model with Employing = willEmployThisYear and pension is None (AND default Employing to None) for pre incorp in before tax year range" in {
      testService(newTaxYearDateInRange).apiToView(willEmployThisYearApiModel, None) mustBe willEmployThisYearViewModel.copy(employingAnyone = None, companyPension = None)
    }
    "return corresponding EmploymentStaff View model with Employing = willEmployNextYear (AND default Employing to None) for pre incorp" in {
      testService().apiToView(willEmployNextYearApiModel, None) mustBe willEmployNextYearViewModelCTY.copy(employingAnyone = None, companyPension = None)
    }
    "return corresponding EmploymentStaff View model with Employing = willEmployNextYear (AND default Employing to None) for pre incorp in before next tax year range" in {
      testService(newTaxYearDateInRange).apiToView(willEmployNextYearApiModel, None) mustBe willEmployNextYearViewModel.copy(employingAnyone = None, companyPension = None)
    }
  }

  "fetchEmploymentView" should {
    "return a View model from S4L" in {
      val partialView = EmployingStaff(Some(EmployingAnyone(false, None)), None, None, None, Some(true))

      when(mockS4LService.fetchAndGet[EmployingStaff](any(), any())(any(), any()))
        .thenReturn(Future(Some(partialView)))

      when(mockIncorpInfoService.getIncorporationDate(any(), any())(any()))
        .thenReturn(Future.successful(Some(LocalDate.now)))

      val result = await(testService().fetchEmployingStaff)
      result mustBe partialView
    }

    "return a view model that's been converted from an api model" in {
      when(mockS4LService.fetchAndGet[EmployingStaff](any(), any())(any(), any()))
        .thenReturn(Future(None))

      when(mockPayeRegistrationConnector.getEmployment(any())(any()))
        .thenReturn(Future(Some(willEmployNextYearApiModel)))

      when(mockIncorpInfoService.getIncorporationDate(any(), any())(any()))
        .thenReturn(Future.successful(Some(LocalDate.now)))

      val result = await(testService(newTaxYearDateInRange).fetchEmployingStaff)
      result mustBe willEmployNextYearViewModel
    }

    "return an empty view model because both S4L and the api returned nothing" in {
      when(mockS4LService.fetchAndGet[EmployingStaff](any(), any())(any(), any()))
        .thenReturn(Future(None))

      when(mockPayeRegistrationConnector.getEmployment(any())(any()))
        .thenReturn(Future(None))

      when(mockIncorpInfoService.getIncorporationDate(any(), any())(any()))
        .thenReturn(Future.successful(Some(LocalDate.now)))

      val result = await(testService().fetchEmployingStaff)
      result mustBe EmployingStaff(None, None, None, None, None)
    }
  }

  "saveEmploymentView" should {
    "return a view model" when {
      "the view model being processed isn't complete and has been saved into S4L" in {
        when(mockS4LService.saveForm[EmployingStaff](any(), any(), any())(any(), any()))
          .thenReturn(Future(Fixtures.blankCacheMap))

        val result = await(testService().saveEmployingStaff("testRegId", EmployingStaff(Some(EmployingAnyone(false, None)), None, None, None, Some(true))))
        result mustBe EmployingStaff(Some(EmployingAnyone(false, None)), None, None, None, Some(true))
      }

      "the view model is complete and the view model has been transformed into an api model and has been saved into the api" in {
        when(mockPayeRegistrationConnector.upsertEmployment(any(), any())(any()))
          .thenReturn(Future(willEmployNextYearApiModel))

        when(mockS4LService.clear(any())(any()))
          .thenReturn(Future(HttpResponse(200, "")))

        val result = await(testService().saveEmployingStaff("testRegId", willEmployNextYearViewModel))
        result mustBe willEmployNextYearViewModel
      }
    }
  }

  "fetchAndUpdateViewModel" should {
    "return a view model" in {
      when(mockS4LService.fetchAndGet[EmployingStaff](any(), any())(any(), any()))
        .thenReturn(Future(Some(alreadyEmployingViewModel)))

      when(mockPayeRegistrationConnector.upsertEmployment(any(), any())(any()))
        .thenReturn(Future(alreadyEmployingApiModel))

      when(mockS4LService.clear(any())(any()))
        .thenReturn(Future(HttpResponse(200, "")))

      await(testService().fetchAndUpdateViewModel(identity)) mustBe alreadyEmployingViewModel
    }
  }

  val partialView = EmployingStaff(None, None, None, None, None)

  Seq((
    "saveEmployingAnyone",
    () => testService().saveEmployingAnyone(EmployingAnyone(true, Some(anotherDateEntered))),
    partialView.copy(employingAnyone = Some(EmployingAnyone(true, Some(anotherDateEntered))))
  ), (
    "saveWillEmployAnyone",
    () => testService().saveWillEmployAnyone(WillBePaying(false, None)),
    partialView.copy(willBePaying = Some(WillBePaying(false, None)))
  ), (
    "saveConstructionIndustry",
    () => testService().saveConstructionIndustry(construction = true),
    partialView.copy(construction = Some(true))
  ), (
    "saveSubcontractors",
    () => testService().saveSubcontractors(subcontractors = true),
    partialView.copy(subcontractors = Some(true))
  ), (
    "savePensionPayment",
    () => testService().savePensionPayment(companyPension = true),
    partialView.copy(companyPension = Some(true))
  )
  ) foreach {
    case (functionName, function, expected) =>
      s"$functionName" should {
        "return a view model" in {

          when(mockS4LService.fetchAndGet[EmployingStaff](any(), any())(any(), any()))
            .thenReturn(Future(None))

          when(mockPayeRegistrationConnector.getEmployment(any())(any()))
            .thenReturn(Future(None))

          when(mockS4LService.saveForm[EmployingStaff](any(), any(), any())(any(), any()))
            .thenReturn(Future(Fixtures.blankCacheMap))

          when(mockIncorpInfoService.getIncorporationDate(any(), any())(any()))
            .thenReturn(Future.successful(Some(LocalDate.now)))

          await(function()) mustBe expected
        }
      }
  }
}