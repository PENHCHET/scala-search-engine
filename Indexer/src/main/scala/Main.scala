import akka.actor.{ActorSystem, Props}
import actor.IndexingActor
import akka.routing.{RoundRobinPool, DefaultResizer}

object Main extends App {
  implicit val system = ActorSystem("IndexerSystem")
  val resizer = DefaultResizer(lowerBound = 1, upperBound = 50)
  val indexingPool = system.actorOf(RoundRobinPool(1, Some(resizer)).props(Props[IndexingActor]), "IndexingPool")
}
