/*
 * Copyright 2024 HM Revenue & Customs
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

//package models
//
//import play.api.libs.json._
//import queries.{Derivable, Gettable, Settable}
//import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
//
//import java.time.Instant
//import scala.util.{Failure, Success, Try}
//
//final case class SubmittedReturnUserAnswers(
//                                             data: JsObject = Json.obj(),
//                                             lastUpdated: Instant = Instant.now
//                                           ) {
//
//  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
//    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)
//
//  def get[A, B](derivable: Derivable[A, B])(implicit rds: Reads[A]): Option[B] = {
//    Reads.optionNoError(Reads.at(derivable.path))
//      .reads(data)
//      .getOrElse(None)
//      .map(derivable.derive)
//  }
//
//  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[SubmittedReturnUserAnswers] = {
//
//    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
//      case JsSuccess(jsValue, _) =>
//        Success(jsValue)
//      case JsError(errors) =>
//        Failure(JsResultException(errors))
//    }
//
//    updatedData.flatMap {
//      d =>
//        val updatedAnswers = copy(data = d)
//        page.cleanup(Some(value), updatedAnswers)
//    }
//  }
//
//  def isDefined(gettable: Gettable[_]): Boolean =
//    Reads.optionNoError(Reads.at[JsValue](gettable.path)).reads(data)
//      .map(_.isDefined)
//      .getOrElse(false)
//
//  def remove[A](page: Settable[A]): Try[SubmittedReturnUserAnswers] = {
//
//    val updatedData = data.removeObject(page.path) match {
//      case JsSuccess(jsValue, _) =>
//        Success(jsValue)
//      case JsError(_) =>
//        Success(data)
//    }
//
//    updatedData.flatMap {
//      d =>
//        val updatedAnswers = copy(data = d)
//        page.cleanup(None, updatedAnswers)
//    }
//  }
//}
//
//object SubmittedReturnUserAnswers {
//
//  val reads: Reads[SubmittedReturnUserAnswers] = {
//
//    import play.api.libs.functional.syntax._
//
//    (
//      (__ \ "data").read[JsObject] and
//        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
//      )(SubmittedReturnUserAnswers.apply _)
//  }
//
//  val writes: OWrites[SubmittedReturnUserAnswers] = {
//
//    import play.api.libs.functional.syntax._
//
//    (
//      (__ \ "data").write[JsObject] and
//        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
//      )(unlift(SubmittedReturnUserAnswers.unapply))
//  }
//
//  implicit val format: OFormat[SubmittedReturnUserAnswers] = OFormat(reads, writes)
//}
