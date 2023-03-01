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

package common.exceptions

object DownstreamExceptions extends DownstreamExceptions

sealed trait DownstreamExceptionsType
sealed trait CurrentProfileNotFoundExceptionType extends DownstreamExceptionsType
sealed trait UnexpectedExceptionType extends DownstreamExceptionsType
trait DownstreamExceptions {

  class CurrentProfileNotFoundException extends Exception with CurrentProfileNotFoundExceptionType

  class PAYEMicroserviceException(msg: String) extends Exception(msg)

  class OfficerListNotFoundException extends Exception

  class S4LFetchException(msg: String) extends Exception(msg)

  class PPOBAddressNotFoundException extends Exception

  class ConfirmationRefsNotFoundException extends Exception

  class IncorporationInformationResponseException(msg: String) extends Exception(msg)

  class CompanyRegistrationException(msg: String) extends Exception(msg)

  class BusinessRegistrationException(msg: String) extends Exception(msg)

  class AddressLookupException(msg: String) extends Exception(msg)

  class UnexpectedException(msg: String) extends Exception(msg) with UnexpectedExceptionType

}
