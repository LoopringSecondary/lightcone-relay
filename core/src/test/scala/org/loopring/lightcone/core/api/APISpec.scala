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

package org.loopring.lightcone.core.api

import io.github.shogowada.scala.jsonrpc.serializers.CirceJSONSerializer
import io.github.shogowada.scala.jsonrpc.server.JSONRPCServer
import org.loopring.lightcone.core.ethaccessor.timeout
import org.loopring.lightcone.gateway.jsonrpc.JsonRpcService
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.util.Success

//////import scala.meta.jsonrpc.testkit.TestConnection
//////import scala.meta.jsonrpc.{ Endpoint, LanguageClient, Services }
//////import java.util.concurrent.ConcurrentLinkedQueue
////
////import monix.execution.Scheduler.Implicits.global
//
import scala.concurrent.{ ExecutionContext, Promise, Future }
//import scribe.Logger

trait LoggerAPI {
  def log(message: String): Unit
}

case class Foo(id: String)

trait FooRepositoryAPI {
  def add(foo: Foo): Future[String]
  def remove(foo: Foo): Future[Unit]
  def getAll(): Future[Set[Foo]]
}

class LoggerAPIImpl extends LoggerAPI {
  override def log(message: String): Unit = println(message)
}

class FooRepositoryAPIImpl extends FooRepositoryAPI {

  implicit val context: ExecutionContext = ExecutionContext.global
  var foos: Set[Foo] = Set()

  override def add(foo: Foo): Future[String] = this.synchronized {
    foos = foos + foo
    println("--------------------> xxxxxxxxxx")
    Future("-------------------> fuckkkkkkkkkk") // Acknowledge
  }

  override def remove(foo: Foo): Future[Unit] = this.synchronized {
    foos = foos - foo
    Future() // Acknowledge
  }

  override def getAll(): Future[Set[Foo]] = Future {
    foos
  }
}

class APISpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *APISpec'] to check api logic")
  implicit val context: ExecutionContext = ExecutionContext.global

  "api" should "xxxxxxxxxxxxxxx" in {

    //    val server = JSONRPCServer(CirceJSONSerializer())
    //    server.bindAPI[LoggerAPI](new LoggerAPIImpl)
    //    server.bindAPI[FooRepositoryAPI](new FooRepositoryAPIImpl)
    //
    //    def onRequestJSONReceived(requestJSON: String): Unit = {
    //      val s = server.receive(requestJSON)
    //
    //      val tx = Await.result(s, timeout.duration)
    //      println("1-------------")
    //      println(tx)
    //      println("2-------------")
    //    }
    //
    //    println("aaaaaaaaaaaaaaa")
    //    onRequestJSONReceived("{\"method\":\"add\", \"params\" : {\"id\" :\"123\", \"delegateAddres\" : \"0xdkfjkdjfkj\"}, \"id\" : 0, \"jsonrpc\":\"2.0\"}")
    //    onRequestJSONReceived("{\"method\":\"getAll\", \"params\" : {\"id\" :\"123\", \"delegateAddres\" : \"0xdkfjkdjfkj\"}, \"id\" : 0, \"jsonrpc\":\"2.0\"}")
    //    println("bbbbbbbbbbbbb")

    val service = new JsonRpcService
    val balanceReq = """{"method":"getBalance", "params" : [{"owner" : "0x17233e07c67d086464fD408148c3ABB56245FA64", "delegateAddress" : "0xdkfjkdjfkj"}], "id" : 0, "jsonrpc":"2.0"}"""

    //    val req = """{"method":"ping", "params" : {"ping" :"aaaaaa"},"id" : 0, "jsonrpc":"2.0"}"""
    //    val req2 = """{"method":"ping", "params" : ["aaaaaa"],"id" : 0, "jsonrpc":"2.0"}"""
    //    val rst = service.getAPIResult(req)
    //    val rst2 = service.getAPIResult(req2)
    val rst3 = service.getAPIResult(balanceReq)

    //    Await.result(rst, timeout.duration)
    //    Await.result(rst2, timeout.duration)
    Await.result(rst3, timeout.duration)

    //    println(rst)
    //    println(rst2)
    println(rst3)

    //    onRequestJSONReceived("{\"method\" : \"test\"}")

    //    val Ping = Endpoint.notification[String]("ping")
    //    val Pong = Endpoint.notification[String]("pong")
    //    val Hello = Endpoint.request[String, String]("hello")
    //
    //    val promise = Promise[Unit]()
    //    val pongs = new ConcurrentLinkedQueue[String]()
    //    val services = Services
    //      .empty(Logger.root)
    //      .request(Hello) { msg ⇒
    //        s"$msg, World!"
    //      }
    //      .notification(Pong) { message ⇒
    //        assert(pongs.add(message))
    //        if (pongs.size() == 2) {
    //          promise.complete(util.Success(()))
    //        }
    //      }
    //    val pongBack: LanguageClient ⇒ Services = { client ⇒
    //      services.notification(Ping) { message ⇒
    //        Pong.notify(message.replace("Ping", "Pong"))(client)
    //      }
    //    }
    //    val conn = TestConnection(pongBack, pongBack)
    //    for {
    //      _ ← Ping.notify("Ping from client")(conn.alice.client)
    //      _ ← Ping.notify("Ping from server")(conn.bob.client)
    //      Right(helloWorld) ← Hello.request("Hello")(conn.alice.client).runAsync
    //      _ ← promise.future
    //    } yield {
    //      //      assertEquals(helloWorld, "Hello, World!")
    //      //      val obtainedPongs = pongs.asScala.toList.sorted
    //      val expectedPongs = List("Pong from client", "Pong from server")
    //      //      assertEquals(obtainedPongs, expectedPongs)
    //      conn.cancel()
    //    }

  }
}
