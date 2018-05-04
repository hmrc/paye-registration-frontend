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

package forms.test

import java.time.LocalDate

import enums.PAYEStatus
import helpers.PayeComponentSpec
import models.api._
import models.view.PAYEContactDetails
import models.{Address, DigitalContactDetails}
import play.api.data.FormError

class TestPAYERegSetupFormSpec extends PayeComponentSpec {
  val testForm = TestPAYERegSetupForm.form

  "Binding TestPAYERegSetupForm to a model" when {
    "Bind successfully with full data" should {
      val data = Map(
        s"registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.tradingName" -> "NEWTEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.line3" -> "Testshire",
        "companyDetails.roAddress.line4" -> "Testford",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line3" -> "Testshire",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.mobileNumber" -> "1234567",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "true",
        "employment.companyPension" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "employmentInfo.alreadyEmploying " -> "true",
        "employmentInfo.pensions" -> "false",
        "employmentInfo.cis" -> "true",
        "employmentInfo.subcontractors" -> "true",
        "employmentInfo.earliestDateDay" -> "1",
        "employmentInfo.earliestDateMonth" -> "1",
        "employmentInfo.earliestDateYear" -> "2017",
        "sicCodes[0].code" -> "84",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "sicCodes[1].description" -> "laundring",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.middleName" -> "Dom",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].name.title" -> "Sir",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.middleName" -> "Michel",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].name.title" -> "Mr",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.payeContactDetails.digitalContactDetails.phoneNumber" -> "0998654",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.line3" -> "Testshire",
        "payeContact.correspondenceAddress.line4" -> "Testford",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST",
        "payeContact.correspondenceAddress.country" -> "UK"
      )

      val model = PAYERegistration(
        registrationID = "54321",
        transactionID = "10-1028374",
        formCreationTimestamp = "01/01/2017",
        status = PAYEStatus.draft,
        completionCapacity = "Director",
        companyDetails = CompanyDetails(
          companyName = "TEST LTD",
          tradingName = Some("NEWTEST LTD"),
          roAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = Some("Testshire"),
            line4 = Some("Testford"),
            postCode = Some("TE1 1ST"),
            country = Some("UK")
          ),
          ppobAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = Some("Testshire"),
            line4 = Some("Testford"),
            postCode = Some("TE1 1ST"),
            country = Some("UK")
          ),
          businessContactDetails = DigitalContactDetails(
            email = Some("test@test.com"),
            mobileNumber = Some("1234567"),
            phoneNumber = Some("099876545")
          )
        ),
        employment = Some(Employment(
          employees = true,
          companyPension = Some(false),
          subcontractors = true,
          firstPayDate = LocalDate.of(2017, 1, 1)
        )),
        employmentInfo = Some(EmploymentV2(
          employees = Employing.alreadyEmploying,
          firstPaymentDate = LocalDate.of(2017, 1, 1),
          construction = true,
          companyPension = Some(false),
          subcontractors = true
        )),
        sicCodes = List(
          SICCode(Some("84"), Some("consulting")),
          SICCode(Some("150"), Some("laundring"))
        ),
        directors = List(
          Director(Name(Some("Thierry"), Some("Dom"), "Henry", Some("Sir")), Some("ZZ12345C")),
          Director(Name(Some("David"), Some("Michel"), "Trezeguet", Some("Mr")), Some("SR12345A"))
        ),
        payeContact = PAYEContact(
          contactDetails = PAYEContactDetails(
            name = "tata",
            digitalContactDetails = DigitalContactDetails(
              email = Some("tata@test.com"),
              mobileNumber = Some("123456"),
              phoneNumber = Some("0998654")
            )
          ),
          correspondenceAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = Some("Testshire"),
            line4 = Some("Testford"),
            postCode = Some("TE1 1ST"),
            country = Some("UK")
          )
        )
      )

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel mustBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data mustBe Map(
          "registrationID" -> "54321",
          "transactionID" -> "10-1028374",
          "formCreationTimestamp" -> "01/01/2017",
          "status" -> "draft",
          "completionCapacity" -> "Director",
          "companyDetails.companyName" -> "TEST LTD",
          "companyDetails.tradingName" -> "NEWTEST LTD",
          "companyDetails.roAddress.line1" -> "Testing Bld",
          "companyDetails.roAddress.line2" -> "1 Test Street",
          "companyDetails.roAddress.line3" -> "Testshire",
          "companyDetails.roAddress.line4" -> "Testford",
          "companyDetails.roAddress.postCode" -> "TE1 1ST",
          "companyDetails.roAddress.country" -> "UK",
          "companyDetails.ppobAddress.line1" -> "Testing Bld",
          "companyDetails.ppobAddress.line2" -> "1 Test Street",
          "companyDetails.ppobAddress.line3" -> "Testshire",
          "companyDetails.ppobAddress.line4" -> "Testford",
          "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
          "companyDetails.ppobAddress.country" -> "UK",
          "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
          "companyDetails.businessContactDetails.mobileNumber" -> "1234567",
          "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
          "employment.employees" -> "true",
          "employment.companyPension" -> "false",
          "employment.subcontractors" -> "true",
          "employment.firstPayDateDay" -> "1",
          "employment.firstPayDateMonth" -> "1",
          "employment.firstPayDateYear" -> "2017",
          "employmentInfo.alreadyEmploying " -> "true",
          "employmentInfo.pensions" -> "false",
          "employmentInfo.cis" -> "true",
          "employmentInfo.subcontractors" -> "true",
          "employmentInfo.earliestDateDay" -> "1",
          "employmentInfo.earliestDateMonth" -> "1",
          "employmentInfo.earliestDateYear" -> "2017",
          "sicCodes[0].code" -> "84",
          "sicCodes[0].description" -> "consulting",
          "sicCodes[1].code" -> "150",
          "sicCodes[1].description" -> "laundring",
          "directors[0].name.firstName" -> "Thierry",
          "directors[0].name.middleName" -> "Dom",
          "directors[0].name.lastName" -> "Henry",
          "directors[0].name.title" -> "Sir",
          "directors[0].nino" -> "ZZ12345C",
          "directors[1].name.firstName" -> "David",
          "directors[1].name.middleName" -> "Michel",
          "directors[1].name.lastName" -> "Trezeguet",
          "directors[1].name.title" -> "Mr",
          "directors[1].nino" -> "SR12345A",
          "payeContact.payeContactDetails.name" -> "tata",
          "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
          "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
          "payeContact.payeContactDetails.digitalContactDetails.phoneNumber" -> "0998654",
          "payeContact.correspondenceAddress.line1" -> "Testing Bld",
          "payeContact.correspondenceAddress.line2" -> "1 Test Street",
          "payeContact.correspondenceAddress.line3" -> "Testshire",
          "payeContact.correspondenceAddress.line4" -> "Testford",
          "payeContact.correspondenceAddress.postCode" -> "TE1 1ST",
          "payeContact.correspondenceAddress.country" -> "UK"
        )
      }
    }

    "Bind successfully with not full data" should {
      val data = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.tradingName" -> "",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.line3" -> "",
        "companyDetails.roAddress.line4" -> "",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line3" -> "",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.mobileNumber" -> "",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.companyPension" -> "",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "employmentInfo.alreadyEmploying " -> "true",
        "employmentInfo.pensions" -> "false",
        "employmentInfo.cis" -> "true",
        "employmentInfo.subcontractors" -> "true",
        "employmentInfo.earliestDateDay" -> "1",
        "employmentInfo.earliestDateMonth" -> "1",
        "employmentInfo.earliestDateYear" -> "2017",
        "sicCodes[0].code" -> "",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "sicCodes[1].description" -> "",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.middleName" -> "",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].name.title" -> "",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.middleName" -> "",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].name.title" -> "",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.payeContactDetails.digitalContactDetails.phoneNumber" -> "",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.line3" -> "",
        "payeContact.correspondenceAddress.line4" -> "",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST",
        "payeContact.correspondenceAddress.country" -> ""
      )

      val model = PAYERegistration(
        registrationID = "54321",
        transactionID = "10-1028374",
        formCreationTimestamp = "01/01/2017",
        status = PAYEStatus.draft,
        completionCapacity = "Director",
        companyDetails = CompanyDetails(
          companyName = "TEST LTD",
          tradingName = None,
          roAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = None,
            line4 = None,
            postCode = Some("TE1 1ST"),
            country = Some("UK")
          ),
          ppobAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = None,
            line4 = Some("Testford"),
            postCode = Some("TE1 1ST"),
            country = Some("UK")
          ),
          businessContactDetails = DigitalContactDetails(
            email = Some("test@test.com"),
            mobileNumber = None,
            phoneNumber = Some("099876545")
          )
        ),
        employment = Some(Employment(
          employees = true,
          companyPension = Some(false),
          subcontractors = true,
          firstPayDate = LocalDate.of(2017, 1, 1)
        )),
        employmentInfo = Some(EmploymentV2(
          employees = Employing.alreadyEmploying,
          firstPaymentDate = LocalDate.of(2017, 1, 1),
          construction = true,
          companyPension = Some(false),
          subcontractors = true
        )),
        sicCodes = List(
          SICCode(None, Some("consulting")),
          SICCode(Some("150"), None)
        ),
        directors = List(
          Director(Name(Some("Thierry"), None, "Henry", None), Some("ZZ12345C")),
          Director(Name(Some("David"), None, "Trezeguet", None), Some("SR12345A"))
        ),
        payeContact = PAYEContact(
          contactDetails = PAYEContactDetails(
            name = "tata",
            digitalContactDetails = DigitalContactDetails(
              email = Some("tata@test.com"),
              mobileNumber = Some("123456"),
              phoneNumber = None
            )
          ),
          correspondenceAddress = Address(
            line1 = "Testing Bld",
            line2 = "1 Test Street",
            line3 = None,
            line4 = None,
            postCode = Some("TE1 1ST"),
            country = None
          )
        )
      )

      "Bind successfully" in {
        val boundModel = testForm.bind(data).fold(
          errors => errors,
          success => success
        )
        boundModel mustBe model
      }

      "Unbind successfully" in {
        val form = testForm.fill(model)
        form.data mustBe Map(
          "registrationID" -> "54321",
          "transactionID" -> "10-1028374",
          "formCreationTimestamp" -> "01/01/2017",
          "status" -> "draft",
          "completionCapacity" -> "Director",
          "companyDetails.companyName" -> "TEST LTD",
          "companyDetails.roAddress.line1" -> "Testing Bld",
          "companyDetails.roAddress.line2" -> "1 Test Street",
          "companyDetails.roAddress.postCode" -> "TE1 1ST",
          "companyDetails.roAddress.country" -> "UK",
          "companyDetails.ppobAddress.line1" -> "Testing Bld",
          "companyDetails.ppobAddress.line2" -> "1 Test Street",
          "companyDetails.ppobAddress.line4" -> "Testford",
          "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
          "companyDetails.ppobAddress.country" -> "UK",
          "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
          "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
          "employment.employees" -> "false",
          "employment.subcontractors" -> "true",
          "employment.firstPayDateDay" -> "1",
          "employment.firstPayDateMonth" -> "1",
          "employment.firstPayDateYear" -> "2017",
          "employmentInfo.alreadyEmploying " -> "true",
          "employmentInfo.pensions" -> "false",
          "employmentInfo.cis" -> "true",
          "employmentInfo.subcontractors" -> "true",
          "employmentInfo.earliestDateDay" -> "1",
          "employmentInfo.earliestDateMonth" -> "1",
          "employmentInfo.earliestDateYear" -> "2017",
          "sicCodes[0].description" -> "consulting",
          "sicCodes[1].code" -> "150",
          "directors[0].name.firstName" -> "Thierry",
          "directors[0].name.lastName" -> "Henry",
          "directors[0].nino" -> "ZZ12345C",
          "directors[1].name.firstName" -> "David",
          "directors[1].name.lastName" -> "Trezeguet",
          "directors[1].nino" -> "SR12345A",
          "payeContact.payeContactDetails.name" -> "tata",
          "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
          "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
          "payeContact.correspondenceAddress.line1" -> "Testing Bld",
          "payeContact.correspondenceAddress.line2" -> "1 Test Street",
          "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
        )
      }
    }
  }

  "Have the correct error" when {
    "registrationID is not completed" in {
      val data: Map[String, String] = Map(
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "completionCapacity" -> "Director",
        "status" -> "draft",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val regIDError = FormError("registrationID", "error.required")

      boundForm.errors mustBe Seq(regIDError)
      boundForm.data mustBe data
    }

    "formCreationTimestamp is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "completionCapacity" -> "Director",
        "status" -> "draft",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val formTimestampError = FormError("formCreationTimestamp", "error.required")

      boundForm.errors mustBe Seq(formTimestampError)
      boundForm.data mustBe data
    }

    "completionCapacity is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val completionCapacityError = FormError("completionCapacity", "error.required")

      boundForm.errors mustBe Seq(completionCapacityError)
      boundForm.data mustBe data
    }

    "company name is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val companyMameError = FormError("companyDetails.companyName", "error.required")

      boundForm.errors mustBe Seq(companyMameError)
      boundForm.data mustBe data
    }

    "roAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val formErrorRoAddressLine1 = FormError("companyDetails.roAddress.line1", "error.required")
      val formErrorRoAddressLine2 = FormError("companyDetails.roAddress.line2", "error.required")

      boundForm.error("companyDetails.roAddress.line1") mustBe Some(formErrorRoAddressLine1)
      boundForm.error("companyDetails.roAddress.line2") mustBe Some(formErrorRoAddressLine2)
      boundForm.data mustBe data
    }

    "ppobAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val formErrorPPOBAddressLine1 = FormError("companyDetails.ppobAddress.line1", "error.required")
      val formErrorPPOBAddressLine2 = FormError("companyDetails.ppobAddress.line2", "error.required")

      boundForm.error("companyDetails.ppobAddress.line1") mustBe Some(formErrorPPOBAddressLine1)
      boundForm.error("companyDetails.ppobAddress.line2") mustBe Some(formErrorPPOBAddressLine2)
      boundForm.data mustBe data
    }

    "employees is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val employeesError = FormError("employment.employees", "error.required")

      boundForm.errors mustBe Seq(employeesError)
      boundForm.data mustBe data
    }

    "subcontractors is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val subcontractorsError = FormError("employment.subcontractors", "error.required")

      boundForm.errors mustBe Seq(subcontractorsError)
      boundForm.data mustBe data
    }

    "firstPayment Day is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val firstPaymentDayError = FormError("employment.firstPayDateDay", "pages.firstPayment.date.invalidRange")

      boundForm.error("employment.firstPayDateDay") mustBe Some(firstPaymentDayError)
      boundForm.data mustBe data
    }

    "firstPayment Month is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val firstPaymentDayError = FormError("employment.firstPayDateDay", "pages.firstPayment.date.invalidRange")

      boundForm.error("employment.firstPayDateDay") mustBe Some(firstPaymentDayError)
      boundForm.data mustBe data
    }

    "firstPayment Year is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val firstPaymentDayError = FormError("employment.firstPayDateDay", "pages.firstPayment.date.invalidRange")

      boundForm.error("employment.firstPayDateDay") mustBe Some(firstPaymentDayError)
      boundForm.data mustBe data
    }

    "paye contact name is not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line1" -> "Testing Bld",
        "companyDetails.ppobAddress.line2" -> "1 Test Street",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.line1" -> "Testing Bld",
        "payeContact.correspondenceAddress.line2" -> "1 Test Street",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val payeContactNameError = FormError("payeContact.payeContactDetails.name", "error.required")

      boundForm.errors mustBe Seq(payeContactNameError)
      boundForm.data mustBe data
    }

    "correspondenceAddress line1 and line2 are not completed" in {
      val data: Map[String, String] = Map(
        "registrationID" -> "54321",
        "transactionID" -> "10-1028374",
        "formCreationTimestamp" -> "01/01/2017",
        "status" -> "draft",
        "completionCapacity" -> "Director",
        "companyDetails.companyName" -> "TEST LTD",
        "companyDetails.roAddress.line1" -> "Testing Bld",
        "companyDetails.roAddress.line2" -> "1 Test Street",
        "companyDetails.roAddress.postCode" -> "TE1 1ST",
        "companyDetails.roAddress.country" -> "UK",
        "companyDetails.ppobAddress.line4" -> "Testford",
        "companyDetails.ppobAddress.postCode" -> "TE1 1ST",
        "companyDetails.ppobAddress.country" -> "UK",
        "companyDetails.businessContactDetails.businessEmail" -> "test@test.com",
        "companyDetails.businessContactDetails.phoneNumber" -> "099876545",
        "employment.employees" -> "false",
        "employment.subcontractors" -> "true",
        "employment.firstPayDateDay" -> "1",
        "employment.firstPayDateMonth" -> "1",
        "employment.firstPayDateYear" -> "2017",
        "sicCodes[0].description" -> "consulting",
        "sicCodes[1].code" -> "150",
        "directors[0].name.firstName" -> "Thierry",
        "directors[0].name.lastName" -> "Henry",
        "directors[0].nino" -> "ZZ12345C",
        "directors[1].name.firstName" -> "David",
        "directors[1].name.lastName" -> "Trezeguet",
        "directors[1].nino" -> "SR12345A",
        "payeContact.payeContactDetails.name" -> "tata",
        "payeContact.payeContactDetails.digitalContactDetails.email" -> "tata@test.com",
        "payeContact.payeContactDetails.digitalContactDetails.mobileNumber" -> "123456",
        "payeContact.correspondenceAddress.postCode" -> "TE1 1ST"
      )
      val boundForm = testForm.bind(data)
      val formErrorCorrespondenceAddressLine1 = FormError("payeContact.correspondenceAddress.line1", "error.required")
      val formErrorCorrespondenceAddressLine2 = FormError("payeContact.correspondenceAddress.line2", "error.required")

      boundForm.error("payeContact.correspondenceAddress.line1") mustBe Some(formErrorCorrespondenceAddressLine1)
      boundForm.error("payeContact.correspondenceAddress.line2") mustBe Some(formErrorCorrespondenceAddressLine2)
      boundForm.data mustBe data
    }
  }
}
