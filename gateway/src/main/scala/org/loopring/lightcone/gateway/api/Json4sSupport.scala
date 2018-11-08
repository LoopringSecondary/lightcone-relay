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

package org.loopring.lightcone.gateway.api

import org.mybatis.scala.mapping.SelectListBy

trait Json4sSupport extends de.heikoseeberger.akkahttpjson4s.Json4sSupport {

  implicit val serialization = org.json4s.native.Serialization
  implicit val formats = org.json4s.DefaultFormats

  // Model beans =================================================================

  class Person {
    var id: Int = _
    var firstName: String = _
    var lastName: String = _
  }

  // Data access layer ===========================================================

  object DB {

    // Simple select function
    val findAll = new SelectListBy[String, Person] {
      def xsql =
        <xsql>
          <bind name="pattern" value="'%' + _parameter + '%'"/>
          SELECT
          id_ as id,
          first_name_ as firstName,
          last_name_ as lastName
          FROM
          person
          WHERE
          first_name_ LIKE #{{pattern}}
        </xsql>
    }

    def bind = Seq(findAll)

  }

}

//// Application code ============================================================
//
//object SelectSample {
//
//  // Do the Magic ...
//  def main(args: Array[String]): Unit = {
//    DB.context.transaction { implicit session ⇒
//
//      DBSchema.create
//      DBSampleData.populate
//
//      val ss = DB.findAll("a")
//
//      DB.findAll("a").foreach { p ⇒
//        println("Person(%d): %s %s".format(p.id, p.firstName, p.lastName))
//      }
//
//    }
//  }

//}

