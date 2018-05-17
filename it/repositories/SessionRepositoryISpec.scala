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

package repositories

import java.util.UUID

import connectors.KeystoreConnector
import enums.IncorporationStatus
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json, OWrites}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositoryISpec extends IntegrationSpecBase {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "microservice.services.incorporation-information.uri" -> "/test-incorporation-information",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "mongodb.uri" -> s"$mongoUri"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val sId = UUID.randomUUID().toString
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

  def currentProfile(regId: String, txId: String = "someTxID", status: String = "draft") = CurrentProfile(
    registrationID            = regId,
    companyTaxRegistration    = CompanyRegistrationProfile(
      status        = status,
      transactionId = txId,
      ackRefStatus  = None
    ),
    language                  = "en",
    payeRegistrationSubmitted = false,
    incorpStatus = None
  )

  class Setup {
    val repository = new ReactiveMongoRepository(app.configuration, mongo)

    val connector = app.injector.instanceOf[KeystoreConnector]
    await(repository.drop)
    await(repository.ensureIndexes)

    implicit val jsObjWts: OWrites[JsObject] = OWrites(identity)

    def count = await(repository.count)
  }

  "SessionRepository" should {
    "cache" when {
      "given a new currentProfile" in new Setup(){
        val currentProfileData: CurrentProfile = currentProfile("regId")
        count mustBe 0
        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, currentProfileData))
        count mustBe 1
      }
      "given an existing currentProfile" in new Setup(){
        val currentProfileData: CurrentProfile = currentProfile("regId")
        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, currentProfileData))
        count mustBe 1

        val newCP = currentProfile("regId", status = "accepted")
        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, newCP))
        count mustBe 1
        await(connector.fetchAndGet[CurrentProfile]("CurrentProfile")) mustBe Some(newCP)
      }
    }
    "fetch" when {
      "given a currentProfile exists" in new Setup(){
        val currentProfileData: CurrentProfile = currentProfile("regId2")
        val key: String = "CurrentProfile"

        await(connector.cache(key, "regId2", currentProfileData.companyTaxRegistration.transactionId, currentProfileData))

        val res: Option[SessionMap] = await(connector.fetch)
        res.isDefined mustBe true
        res.get.data mustBe Map(key -> Json.toJson(currentProfileData))
      }
    }
    "fetchAndGet" when {
      "given a currentProfile and key" in new Setup(){
        val currentProfileData: CurrentProfile = currentProfile("regId3")
        val key: String = "CurrentProfile"

        await(connector.cache(key, "regId3", currentProfileData.companyTaxRegistration.transactionId, currentProfileData))

        val res: Option[CurrentProfile] = await(connector.fetchAndGet(key)(hc, CurrentProfile.format))
        res.isDefined mustBe true
        res.get mustBe currentProfileData
      }
      "given no current profile" in new Setup(){
        val key: String = "CurrentProfile"

        val res: Option[CurrentProfile] = await(connector.fetchAndGet(key)(hc, CurrentProfile.format))
        res.isDefined mustBe false
      }
    }
    "remove" when {
      "there is a current profile to remove" in new Setup() {
        val currentProfileData = currentProfile("regId")
        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, currentProfileData))
        count mustBe 1

        val res: Boolean = await(connector.remove)
        res mustBe true
        count mustBe 0
      }
      "there is no current profile to remove" in new Setup() {
        val res: Boolean = await(connector.remove)

        res mustBe false
        count mustBe 0
      }

      "there are two current profiles" in new Setup(){
        val currentProfileData = currentProfile("regId")
        val hc1 = hc.copy(sessionId = Some(SessionId("id1")))

        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, currentProfileData)(hc1, CurrentProfile.format))
        await(connector.cache("CurrentProfile", "regId", currentProfileData.companyTaxRegistration.transactionId, currentProfileData)(hc.copy(sessionId = Some(SessionId("id2"))), CurrentProfile.format))
        count mustBe 2

        val res: Boolean = await(connector.remove()(hc1))
        res mustBe true
        count mustBe 1
      }
    }
  }
}
