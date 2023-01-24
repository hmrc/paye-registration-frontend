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

package connectors.httpParsers

import common.exceptions.DownstreamExceptions
import connectors.BaseConnector
import models.Address
import models.external.BusinessProfile
import models.view.{CompanyDetails, PAYEContactDetails}
import play.api.libs.json.{Reads, __}
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads

trait BusinessRegistrationHttpParsers extends BaseHttpReads { _: BaseConnector =>

  override def unexpectedStatusException(url: String, status: Int, regId: Option[String], txId: Option[String]): Exception =
    new DownstreamExceptions.BusinessRegistrationException(s"Calling url: '$url' returned unexpected status: '$status'${logContext(regId, txId)}")

  def businessProfileHttpReads()(implicit request: Request[_]): HttpReads[Option[BusinessProfile]] =
    optionHttpReads("businessProfileHttpReads")

  def retrieveCompletionCapacityHttpReads()(implicit request: Request[_]): HttpReads[Option[String]] = {
    implicit val reads = (__ \ "completionCapacity").read[String]
    optionHttpReads("retrieveCompletionCapacityHttpReads")
  }

  def retrieveTradingNameHttpReads(regId: String)(implicit request: Request[_]): HttpReads[Option[String]] = {
    implicit val reads = CompanyDetails.tradingNameApiPrePopReads
    optionHttpReads("retrieveTradingNameHttpReads", Some(regId), logInfoMsg = true, defaultToNoneOnError = true)
  }

  def upsertTradingNameHttpReads(regId: String, tradingName: String)(implicit request: Request[_]): HttpReads[String] =
    basicUpsertReads("upsertTradingNameHttpReads", tradingName, Some(regId))

  def retrieveContactDetailsHttpReads(regId: String)(implicit request: Request[_]): HttpReads[Option[PAYEContactDetails]] = {
    implicit val reads = PAYEContactDetails.prepopReads
    optionHttpReads("retrieveContactDetailsHttpReads", Some(regId), defaultToNoneOnError = true)
  }

  def upsertContactDetailsHttpReads(regId: String, contactDetails: PAYEContactDetails)(implicit request: Request[_]): HttpReads[PAYEContactDetails] =
    basicUpsertReads("upsertContactDetailsHttpReads", contactDetails, Some(regId))

  def retrieveAddressesHttpReads(regId: String)(implicit request: Request[_]): HttpReads[Seq[Address]] = {
    implicit val reads = (__ \ "addresses").read[Seq[Address]](Reads.seq(Address.prePopReads))
    seqHttpReads("retrieveAddressesHttpReads", Some(regId), defaultToEmptyOnError = true)(reads, manifest[Address], request)
  }

  def upsertAddressHttpReads(regId: String, address: Address)(implicit request: Request[_]): HttpReads[Address] =
    basicUpsertReads("upsertAddressHttpReads", address, Some(regId))
}

object BusinessRegistrationHttpParsers extends BusinessRegistrationHttpParsers with BaseConnector
