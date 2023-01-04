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

package helpers.fixtures

import models.Address
import models.api.{Director, Name}
import models.external._
import models.view.Directors

trait CoHoAPIFixture {

  val validCoHoCompanyDetailsResponse = CoHoCompanyDetailsModel(
    companyName = "Test Company",
    roAddress = Address(
      line1 = "1 Test Road",
      line2 = "Testford",
      line3 = None,
      line4 = None,
      postCode = Some("TE1 1ST")
    )
  )

  val validOfficerList = OfficerList(
    items = Seq(
      Officer(
        name = Name(Some("test1"), Some("test11"), Some("testa"), Some("Mr")),
        role = "cic-manager",
        resignedOn = None,
        appointmentLink = None
      ),
      Officer(
        name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
        role = "director",
        resignedOn = None,
        appointmentLink = None
      )
    )
  )

  val invalidOfficerList = OfficerList(
    items = Seq(
      Officer(
        name = Name(Some("test1"), Some("test11"), Some("testa"), Some("Mr")),
        role = "cic-manager",
        resignedOn = None,
        appointmentLink = None
      ),
      Officer(
        name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
        role = "corporate-director",
        resignedOn = None,
        appointmentLink = None
      )
    )
  )

  val validDirectorDetails = Directors(
    directorMapping = Map(
      "0" -> Director(
        name = Name(Some("test2"), Some("test22"), Some("testb"), Some("Mr")),
        nino = None
      )
    )
  )
}