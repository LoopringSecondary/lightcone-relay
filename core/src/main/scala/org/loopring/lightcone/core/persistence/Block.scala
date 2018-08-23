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

trait BaseEntity {
  val id: Long
  def isValid: Boolean = true
}

case class BlockEntity(
  id: Long,
  blockNumber: String,
  blockHash: String,
  parentHash: String,
  fork: Boolean,
  createTime: Long = 0L) extends BaseEntity {}

trait BlockMapper {
  def selectById(id: Int): BlockEntity
}
