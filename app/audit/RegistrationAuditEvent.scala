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

package audit

import audit.RegistrationAuditEvent.buildTags
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

case class TagSet(clientIP: Boolean,
                  clientPort: Boolean,
                  requestId: Boolean,
                  sessionId: Boolean,
                  deviceId: Boolean,
                  authorisation: Boolean,
                  path: Boolean)

object TagSet {
  val ALL_TAGS = TagSet(true, true, true, true, true, true, true)
  val NO_TAGS = TagSet(false, false, false, false, false, false, false)
  val REQUEST_ONLY = TagSet(false, false, true, false, false, false, false)
}

import audit.TagSet.ALL_TAGS

abstract class RegistrationAuditEvent(auditType: String, transactionName : Option[String], detail: JsObject, tagSet: TagSet = ALL_TAGS)
                                     (implicit hc: HeaderCarrier, optReq: Option[Request[AnyContent]] = None)
  extends ExtendedDataEvent(
    auditSource = "paye-registration-frontend",
    auditType = auditType,
    detail = detail,
    tags = buildTags(transactionName.getOrElse(auditType), tagSet)
  )

object RegistrationAuditEvent {

  val EXTERNAL_USER_ID = "externalUserId"
  val AUTH_PROVIDER_ID = "authProviderId"
  val JOURNEY_ID = "journeyId"
  val PATH = "path"

  def buildTags(transactionName: String, tagSet: TagSet)(implicit hc: HeaderCarrier, optReq: Option[Request[AnyContent]]) = {
    Map("transactionName" -> transactionName) ++
      buildClientIP(tagSet) ++
      buildClientPort(tagSet) ++
      buildRequestId(tagSet) ++
      buildSessionId(tagSet) ++
      buildDeviceId(tagSet) ++
      buildAuthorization(tagSet) ++
      buildPath(tagSet)
  }

  private def buildClientIP(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.clientIP) Map("clientIP" -> hc.trueClientIp.getOrElse("-")) else Map()

  private def buildClientPort(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.clientPort) Map("clientPort" -> hc.trueClientPort.getOrElse("-")) else Map()

  private def buildRequestId(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.requestId) Map(hc.names.xRequestId -> hc.requestId.map(_.value).getOrElse("-")) else Map()

  private def buildSessionId(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.sessionId) Map(hc.names.xSessionId -> hc.sessionId.map(_.value).getOrElse("-")) else Map()

  private def buildAuthorization(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.authorisation) Map(hc.names.authorisation -> hc.authorization.map(_.value).getOrElse("-")) else Map()

  private def buildDeviceId(tagSet: TagSet)(implicit hc: HeaderCarrier) =
    if(tagSet.deviceId) Map(hc.names.deviceID -> hc.deviceID.getOrElse("-")) else Map()

  private def buildPath(tagSet: TagSet)(implicit optReq: Option[Request[AnyContent]]) = {
    if (tagSet.path) optReq.fold(Map[String, String]())(req => Map(PATH -> req.path)) else Map()
  }
}
