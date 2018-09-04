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

package org.loopring.lightcone.core.persistence

import com.google.inject.Inject
import org.loopring.lightcone.core.persistence.dals._
import slick.basic.DatabaseConfig
import slick.basic.BasicProfile
import slick.jdbc.JdbcProfile

trait PersistenceModule {
  val dbConfig: DatabaseConfig[JdbcProfile]

  def generateDDL(): Unit

  def profile: JdbcProfile = dbConfig.profile
  def db: BasicProfile#Backend#Database = dbConfig.db
}

trait LightconePersistenceModule extends PersistenceModule {
  val orders: OrdersDal
  def generateDDL(): Unit

  def displayDDL(): Unit
}

class LightconePersistenceModuleImpl @Inject() (
  val dbConfig: DatabaseConfig[JdbcProfile]) extends LightconePersistenceModule {
  val orders = new OrdersDalImpl(this)

  def generateDDL(): Unit = {
    Seq(
      orders.createTable()
    )
  }

  def displayDDL(): Unit = {
    orders.displayTableSchema()
  }
}