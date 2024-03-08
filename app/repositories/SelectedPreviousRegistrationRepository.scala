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

package repositories

import config.FrontendAppConfig
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import viewmodels.previousReturns.SelectedPreviousRegistration

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectedPreviousRegistrationRepository @Inject()(
                                                        mongoComponent: MongoComponent,
                                                        appConfig: FrontendAppConfig,
                                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SelectedPreviousRegistration](
    collectionName = "selected-previous-registration",
    mongoComponent = mongoComponent,
    domainFormat = SelectedPreviousRegistration.format,
    replaceIndexes = true,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("userId"),
        IndexOptions()
          .name("userIdIdx")
          .unique(true)
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    )
  ) {

  private def byUserId(id: String): Bson = Filters.equal("userId", id)

  def get(userId: String): Future[Option[SelectedPreviousRegistration]] =
    collection.find(byUserId(userId)).headOption()

  def set(selectedPreviousRegistration: SelectedPreviousRegistration): Future[SelectedPreviousRegistration] = {
    collection
      .replaceOne(
        filter = byUserId(selectedPreviousRegistration.userId),
        replacement = selectedPreviousRegistration,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => selectedPreviousRegistration)
  }

  def clear(id: String): Future[Boolean] =
    collection
      .deleteOne(byUserId(id))
      .toFuture()
      .map(_ => true)
}