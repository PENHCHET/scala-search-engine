import akka.actor.{ActorSystem, Props}
import actor.{RootActor, NewLink}

import util.FileReader.{fileContentLineByLineList => readLines}
import DAO.PageTable

object Main extends App {
  implicit val system = ActorSystem("actor-system")
  val seedList = readLines(system.settings.config.getString("app.seedFile"))
  val rActor = system.actorOf(Props[RootActor], name = "RootActor")

  seedList foreach{
    url => rActor ! NewLink(url, 1, false)
  }
}
