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
import viewmodels.payments.SelectedPrepareData

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectedPrepareDataRepository @Inject()(
                                               mongoComponent: MongoComponent,
                                               appConfig: FrontendAppConfig,
                                               clock: Clock
                                             )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SelectedPrepareData](
    collectionName = "selected-prepare-data",
    mongoComponent = mongoComponent,
    domainFormat = SelectedPrepareData.format,
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

  def get(userId: String): Future[Option[SelectedPrepareData]] =
    collection.find(byUserId(userId)).headOption()

  def set(selectedPrepareData: SelectedPrepareData): Future[SelectedPrepareData] = {
    val updatedPrepareData = selectedPrepareData.copy(lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byUserId(selectedPrepareData.userId),
        replacement = updatedPrepareData,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => updatedPrepareData)
  }

  def clear(id: String): Future[Boolean] =
    collection
      .deleteOne(byUserId(id))
      .toFuture()
      .map(_ => true)
}
