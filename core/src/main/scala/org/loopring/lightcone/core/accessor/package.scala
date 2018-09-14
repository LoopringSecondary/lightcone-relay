/*
 * Copyright 2018 lightcore-relay
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

package org.loopring.lightcone.core

import akka.stream._
import scala.util._
import akka.http.scaladsl.model._
import akka.stream.scaladsl._
import akka.http.scaladsl._
import scala.concurrent.Promise

package object accessor {
  type HttpFlow = Flow[ //
  (HttpRequest, Promise[HttpResponse]), //
  (Try[HttpResponse], Promise[HttpResponse]), //
  Http.HostConnectionPool]
}
