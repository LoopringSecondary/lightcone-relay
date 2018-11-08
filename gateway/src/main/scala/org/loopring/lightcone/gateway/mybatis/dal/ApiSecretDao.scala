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

package org.loopring.lightcone.gateway.mybatis.dal

import org.loopring.lightcone.gateway.mybatis.table.ApiSecret
import org.mybatis.scala.mapping._
import org.mybatis.scala.session._
import org.mybatis.scala.mapping.Binding._

object ApiSecretDao {

  val ApiSecretResultMap = new ResultMap[ApiSecret] {
    id(column = "id", property = "id")
    result(column = "first_name", property = "firstName")
    result(column = "last_name", property = "lastName")
  }

  val findAll = new SelectList[ApiSecret]() {
    resultMap = ApiSecretResultMap

    override def xsql: XSQL = {
      <xsql>
        SELECT * FROM api_secret
      </xsql>
    }
  }

  val dropTable = new Perform {
    def xsql =
      <xsql>
        DROP TABLE IF EXISTS api_secret
      </xsql>
  }

  val createTable = new Perform {
    def xsql =
      <xsql>
        CREATE TABLE IF NOT EXISTS api_secret (
        id INT AUTO_INCREMENT,
        create_at bigint(20),
        update_at bigint(20),
        api_key varchar(255),
        api_secret varchar(255),
        primary key (id)
        ) ENGINE = InnoDB CHARSET=utf8 AUTO_INCREMENT=1;
      </xsql>
  }

  val insertSecret = new Insert[java.util.Map[_, _]] {
    def xsql =
      <xsql>
        INSERT INTO api_secret(id, first_name, last_name)
        VALUES (#{{id}}, #{{firstName}}, #{{lastName}})
      </xsql>
  }

  def initdb(implicit s: Session) = {
    dropTable()
    createTable()
  }

  val insert = new Insert[ApiSecret] {
    def xsql =
      <xsql>
        INSERT INTO api_secret(id, first_name, last_name)
        VALUES (
        { ?("id", jdbcType = JdbcType.INTEGER) }
        ,
        { ?("firstName", jdbcType = JdbcType.VARCHAR) }
        ,
        { ?("lastName", jdbcType = JdbcType.VARCHAR) }
        )
      </xsql>
  }

  def initData(implicit s: Session) = {
    import scala.collection.JavaConverters._
    insertSecret(Map("id" -> 1, "firstName" -> "John", "lastName" -> "Smart").asJava)
    insertSecret(Map("id" -> 2, "firstName" -> "Maria", "lastName" -> "Perez").asJava)
    insertSecret(Map("id" -> 3, "firstName" -> "Janeth", "lastName" -> "Ros").asJava)
    insertSecret(Map("id" -> 4, "firstName" -> "Paul", "lastName" -> "Jobs").asJava)
    insertSecret(Map("id" -> 5, "firstName" -> "Bill", "lastName" -> "Rich").asJava)
  }
  def bind = Seq(findAll, dropTable, createTable, insertSecret, insert)

}
