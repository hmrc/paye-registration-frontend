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

package fixtures

import models.api.{Director, Name}
import models.external._
import models.view.Directors

trait CoHoAPIFixture {

  val validCoHoCompanyDetailsResponse = CoHoCompanyDetailsModel(
    registrationID = "12345",
    companyName = "Test Company",
    areasOfIndustry = Seq(
      AreaOfIndustry(
        sicCode = "100",
        description = "Chips"
      ),
      AreaOfIndustry(
        sicCode = "101",
        description = "Fish"
      )
    )
  )

  val validCHROAddress = CHROAddress(
    premises = "11",
    addressLine1 = "Test Street",
    addressLine2 = Some("Test Area"),
    locality = "Test Town",
    country = Some("Test Country"),
    poBox = Some("Test PO BOX"),
    postalCode = Some("TE1 1ST"),
    region = Some("Test Region")
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
