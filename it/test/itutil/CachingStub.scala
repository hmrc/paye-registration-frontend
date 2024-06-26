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

package itutil

import com.github.tomakehurst.wiremock.client.WireMock._
import models.api.SessionMap
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json._
import repositories.SessionRepository
import uk.gov.hmrc.crypto.{ApplicationCrypto, Sensitive}
import uk.gov.hmrc.crypto.json.JsonEncryption

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.mongo.test.MongoSupport

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait CachingStub extends MongoSupport with BeforeAndAfterEach {
  self: IntegrationSpecBase =>

  case class SensitiveT[T](override val decryptedValue: T) extends Sensitive[T]
  
  implicit lazy val jsonCrypto = new ApplicationCrypto(app.configuration.underlying).JsonCrypto

  implicit lazy val encryptionFormat = JsonEncryption.sensitiveEncrypter[JsObject, SensitiveT[JsObject]]
  lazy val ec: ExecutionContext = ExecutionContext.global
  lazy val repo = new SessionRepository(app.configuration, mongoComponent)(ec)

  def customAwait[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  val defaultTimeout: FiniteDuration = new FiniteDuration(5, TimeUnit.SECONDS)

  override def beforeEach(): Unit = {
    super.beforeEach()
    customAwait(repo.collection.deleteMany(BsonDocument()).toFuture())(defaultTimeout)
    await(repo.collection.countDocuments().toFuture()) mustBe 0
    resetWiremock()
  }

  def stubSessionCacheMetadata(session: String, regId: String, submitted: Boolean = false) = {
    customAwait(repo.ensureIndexes)(defaultTimeout)
    customAwait(repo.collection.deleteMany(BsonDocument()).toFuture())(defaultTimeout)

    val preawait = customAwait(repo.collection.countDocuments().toFuture())(defaultTimeout)
    val cp = CurrentProfile(
      registrationID = regId,
      companyTaxRegistration =
        CompanyRegistrationProfile(
          status = "submitted",
          transactionId = "12345"),
      language = "ENG",
      payeRegistrationSubmitted = submitted,
      incorpStatus = None
    )
    val currentProfileMapping: Map[String, JsValue] = Map("CurrentProfile" -> Json.toJson(cp))
    val res = customAwait(repo.upsertSessionMap(SessionMap(session, regId, "12345", currentProfileMapping)))(defaultTimeout)
    if (customAwait(repo.collection.countDocuments().toFuture())(defaultTimeout) != preawait + 1) throw new Exception("Error adding data to database")
    res
  }

  def verifySessionCacheData[T](id: String, key: String, data: Option[T])(implicit format: Format[T]): Unit = {
    val dataFromDb = customAwait(repo.getSessionMap(id))(defaultTimeout).flatMap(_.getEntry(key))
    if (data != dataFromDb) throw new Exception(s"Data in database doesn't match expected data:\n expected data $data was not equal to actual data $dataFromDb")
  }

  def stubPayeRegDocumentStatus(regId: String) = {
    val payeRegUrl = s"/paye-registration/$regId/status"
    stubFor(get(urlMatching(payeRegUrl))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "status" : "draft"
               |}
             """.stripMargin
          )
      )
    )
  }

  def stubS4LGet(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(SensitiveT(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(get(urlMatching(s"/save4later/paye-registration-frontend/$regId"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

  def stubS4LPut(regId: String, key: String, data: String) = {
    val s4lData = Json.parse(data).as[JsObject]
    val encData = encryptionFormat.writes(SensitiveT(s4lData)).as[JsString]

    val s4LResponse = Json.obj(
      "id" -> key,
      "data" -> Json.obj(key -> encData)
    )

    stubFor(put(urlMatching(s"/save4later/paye-registration-frontend/$regId/data/$key"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s4LResponse.toString())
      )
    )
  }

}
