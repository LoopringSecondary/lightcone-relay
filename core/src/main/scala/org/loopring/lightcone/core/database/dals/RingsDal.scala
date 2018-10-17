/*
 * Copyright 2018 Loopring Foundation
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

package org.loopring.lightcone.core.database.dals

import org.loopring.lightcone.core.database.OrderDatabase
import org.loopring.lightcone.core.database.base._
import org.loopring.lightcone.core.database.tables._
import org.loopring.lightcone.proto.{ ring ⇒ protoRing }
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.Future

trait RingsDal extends BaseDalImpl[Rings, protoRing.Rings] {
  def getUnblockedRings(untilTime: Long): Future[Seq[protoRing.Rings]] //所有在untilTime暂未打到块里的环路
}

class RingsDalImpl(val module: OrderDatabase) extends RingsDal {
  val query = ringsQ

  override def update(row: protoRing.Rings): Future[Int] = ???

  override def update(rows: Seq[protoRing.Rings]): Future[Unit] = ???

  override def getUnblockedRings(untilTime: Long): Future[Seq[protoRing.Rings]] = {
    val queryCondition = query.filter(_.createdAt > untilTime)
    db.run(queryCondition.result)
  }
}
