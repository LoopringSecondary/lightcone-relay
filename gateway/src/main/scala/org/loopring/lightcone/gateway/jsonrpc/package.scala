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

package org.loopring.lightcone.gateway

package object jsonrpc {

  case class JsonRpcRequest(id: Int, method: String, jsonrpc: String, params: String)

  case class JsonRpcException(code: Int, message: String, id: Option[Int] = None) extends Exception(message)

  case class JsonRpcResponse(
      id: Option[Int] = None.orNull,
      jsonrpc: String = "2.0",
      result: Any = None,
      error: Option[JsonRpcException] = None
  )

}
