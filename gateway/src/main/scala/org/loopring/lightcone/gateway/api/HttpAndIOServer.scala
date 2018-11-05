package org.loopring.lightcone.gateway.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import org.loopring.lightcone.gateway.jsonrpc.JsonRpcServer
import org.loopring.lightcone.gateway.socketio.SocketIOServer
import org.slf4j.LoggerFactory

class HttpAndSocketIOServer @Inject()(
  jsonRpcServer: JsonRpcServer,
  ioServer: SocketIOServer
)(
  implicit
  system: ActorSystem,
  mat: ActorMaterializer) {

  lazy val logger = LoggerFactory.getLogger(getClass)

  implicit val ex = system.dispatcher

  lazy val route =
    pathPrefix("rpc") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[String]) { json ⇒
                // TODO 这里还需要测试 返回的可能是 text 需要转成 application/json
                // 可能需要用 entity 包装
                complete(jsonRpcServer.handleRequest(json))
              }
            }
          )
        }
      )
    }

  lazy val bind = Http().bindAndHandle(route, "localhost", system.settings.config.getInt("jsonrpc.http.port"))

  bind onComplete {
    case scala.util.Success(value) ⇒ logger.info(s"http server has started @ $value")
    case scala.util.Failure(ex) ⇒ logger.error("http server failed", ex)
  }

  try ioServer.start
  catch {
    case t: Throwable ⇒ logger.error("socketio started failed!", t)
  }

}
