import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import actor.{SEServiceActor, SearchManagerActor}

object Main extends App {
  implicit val system = ActorSystem("actor-system")
  implicit val timeout = Timeout(60.seconds)
  val service = system.actorOf(Props[SEServiceActor], "se-service")
  val searchManager = system.actorOf(Props[SearchManagerActor], "se-manager")
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}
