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

package models.test

import models.coHo.{AreaOfIndustry, CoHoCompanyDetailsModel}
import play.api.libs.json.Json

case class CoHoCompanyDetailsFormModel (
                                       companyName: String,
                                       sicCodes: List[String],
                                       descriptions: List[String]
                                         ) {

  def toCoHoCompanyDetailsAPIModel(regID: String) = CoHoCompanyDetailsModel(regID, companyName, constructAreasOfIndustry())

  private def constructAreasOfIndustry(): List[AreaOfIndustry] = {

    val filteredCodes = sicCodes.filterNot(_ == "")
    val filteredDescs = descriptions.filterNot(_ == "")
    if(filteredCodes.length == filteredDescs.length) {
      filteredCodes.zipWithIndex.map { case (sicCode, index) =>
        AreaOfIndustry(sicCode, filteredDescs(index))
      }
    } else {
      throw new Exception("sic codes and descriptions did not match in length in CoHoCompanyDetailsFormModel")
    }
  }
}


object CoHoCompanyDetailsFormModel {
  implicit val formats = Json.format[CoHoCompanyDetailsFormModel]
}
