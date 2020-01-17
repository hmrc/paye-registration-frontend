/*
 * Copyright 2020 HM Revenue & Customs
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

package models.external

import helpers.PayeComponentSpec
import models.api.Name
import play.api.libs.json.Json

class OfficerListSpec extends PayeComponentSpec {
  val tstOfficerList = OfficerList(
    items = Seq(
      Officer(
        name = Name(Some("Bob"), Some("Bimbly Bobblous"), "Bobbings", None),
        role = "director",
        resignedOn = None,
        appointmentLink = None
      ),
      Officer(
        name = Name(Some("Jingly"), None, "Jingles", Some("Mx")),
        role = "director",
        resignedOn = None,
        appointmentLink = None
      ),
      Officer(
        name = Name(Some("Jorge"), None, "Freshwater", None),
        role = "legend",
        resignedOn = None,
        appointmentLink = None
      )
    )
  )

  val tstOfficerListJson = Json.parse(
    """[
      |   {
      |      "name_elements":{
      |         "forename":"Bob",
      |         "other_forenames":"Bimbly Bobblous",
      |         "surname":"Bobbings"
      |      },
      |      "date_of_birth":{
      |         "day":"12",
      |         "month":"11",
      |         "year":"1973"
      |      },
      |      "address":{
      |         "premises":"98",
      |         "address_line_1":"LIMBRICK LANE",
      |         "address_line_2":"GORING-BY-SEA",
      |         "locality":"WORTHING",
      |         "country":"United Kingdom",
      |         "postal_code":"BN12 6AG"
      |      },
      |      "officer_role":"director",
      |      "retired_on":"01/01/2017"
      |   },
      |   {
      |      "name_elements":{
      |         "title":"Mx.",
      |         "forename":"Jingly",
      |         "surname":"Jingles"
      |      },
      |      "date_of_birth":{
      |         "day":"12",
      |         "month":"07",
      |         "year":"1988"
      |      },
      |      "address":{
      |         "premises":"713",
      |         "address_line_1":"ST. JAMES GATE",
      |         "locality":"NEWCASTLE UPON TYNE",
      |         "country":"England",
      |         "postal_code":"NE1 4BB"
      |      },
      |      "officer_role":"director"
      |   },
      |   {
      |      "name_elements":{
      |         "forename":"Jorge",
      |         "surname":"Freshwater"
      |      },
      |      "date_of_birth":{
      |         "day":"10",
      |         "month":"06",
      |         "year":"1994"
      |      },
      |      "address":{
      |         "premises":"1",
      |         "address_line_1":"L ST",
      |         "locality":"TYNE",
      |         "country":"England",
      |         "postal_code":"AA1 4AA"
      |      },
      |      "officer_role":"legend"
      |   }
      |]""".stripMargin
  )

  "OfficerList" should {
    "read from Json" in {
      Json.fromJson[OfficerList](tstOfficerListJson).get mustBe tstOfficerList
    }

    "read and normalize from Json" in {
      val tstOfficerListJson2 = Json.parse(
        """[
          |   {
          |      "name_elements":{
          |         "forename":"Bob",
          |         "other_forenames":"Bimbly Bobblôus",
          |         "surname":"Bobbïngs"
          |      },
          |      "date_of_birth":{
          |         "day":"12",
          |         "month":"11",
          |         "year":"1973"
          |      },
          |      "address":{
          |         "premises":"98",
          |         "address_line_1":"LIMBRICK LANE",
          |         "address_line_2":"GORING-BY-SEA",
          |         "locality":"WORTHING",
          |         "country":"United Kingdom",
          |         "postal_code":"BN12 6AG"
          |      },
          |      "officer_role":"director"
          |   },
          |   {
          |      "name_elements":{
          |         "title":"Mx.",
          |         "forename":"Jingly",
          |         "surname":"Jinglés"
          |      },
          |      "date_of_birth":{
          |         "day":"12",
          |         "month":"07",
          |         "year":"1988"
          |      },
          |      "address":{
          |         "premises":"713",
          |         "address_line_1":"ST. JAMES GATE",
          |         "locality":"NEWCASTLE UPON TYNE",
          |         "country":"England",
          |         "postal_code":"NE1 4BB"
          |      },
          |      "officer_role":"director"
          |   },
          |   {
          |      "name_elements":{
          |         "forename":"Jorgè",
          |         "surname":"Freshwàter"
          |      },
          |      "date_of_birth":{
          |         "day":"10",
          |         "month":"06",
          |         "year":"1994"
          |      },
          |      "address":{
          |         "premises":"1",
          |         "address_line_1":"L ST",
          |         "locality":"TYNE",
          |         "country":"England",
          |         "postal_code":"AA1 4AA"
          |      },
          |      "officer_role":"legend"
          |   }
          |]""".stripMargin
      )

      Json.fromJson[OfficerList](tstOfficerListJson2).get mustBe tstOfficerList
    }
  }
}
