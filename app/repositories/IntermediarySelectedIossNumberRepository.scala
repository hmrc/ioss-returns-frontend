/*
 * Copyright 2026 HM Revenue & Customs
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
import models.IntermediarySelectedIossNumber
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntermediarySelectedIossNumberRepository @Inject()(
                                              mongoComponent: MongoComponent,
                                              appConfig: FrontendAppConfig,
                                              clock: Clock
                                            )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[IntermediarySelectedIossNumber](
    collectionName = "intermediary-selected-ioss-number",
    mongoComponent = mongoComponent,
    domainFormat = IntermediarySelectedIossNumber.format,
    replaceIndexes = true,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("userId"),
        IndexOptions()
          .name("userIdIdx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .unique(false)
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    )
  ) {

  private def byUserId(id: String): Bson = Filters.equal("userId", id)

  def get(userId: String): Future[Option[IntermediarySelectedIossNumber]] =
    collection.find(byUserId(userId)).headOption()

  def set(selectedIossNumber: IntermediarySelectedIossNumber): Future[IntermediarySelectedIossNumber] = {
    val updatedIossNumber = selectedIossNumber.copy(lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byUserId(selectedIossNumber.userId),
        replacement = updatedIossNumber,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => updatedIossNumber)
  }

  def keepAlive(id: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byUserId(id),
        update = Updates.set("lastUpdated", Instant.now(clock)),
      )
      .toFuture()
      .map(_ => true)

  def clear(id: String): Future[Boolean] =
    collection
      .deleteOne(byUserId(id))
      .toFuture()
      .map(_ => true)
}
