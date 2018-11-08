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

package org.loopring.lightcone.gateway.mybatis

import org.apache.ibatis.datasource.pooled.PooledDataSource
import org.mybatis.scala.config.{Configuration, Environment, JdbcTransactionFactory}
import com.typesafe.config.Config
import org.loopring.lightcone.gateway.mybatis.dal.ApiSecretDao

class Conn(dbconfig : Config) {

  val config = Configuration(
    Environment(
      "default",
      new JdbcTransactionFactory(),
      new PooledDataSource(
        dbconfig.getString("db.mybatis.driver"), //  "org.hsqldb.jdbcDriver"
        dbconfig.getString("db.mybatis.url"), // "jdbc:hsqldb:mem:scala"
        dbconfig.getString("db.mybatis.name"), // "lightcone-relay"
        dbconfig.getString("db.mybatis.password"), // "111111"
      )
    )
  )
  // Add the data access method to the default namespace
  config ++= ApiSecretDao

  // Build the session manager
  lazy val context = config.createPersistenceContext
}
