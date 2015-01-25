package actor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.routing.RoundRobinPool
import akka.util.Timeout

import util.FileReader.{fileContentLineByLineList => readLines}

class FrontierManager extends Actor{
  implicit val timeout = Timeout(60.second)
  val workerPool = context.actorOf(RoundRobinPool(3).props(Props[FrontierGuard]), "Frontier_workerPool")
  lazy val conf = context.system.settings.config
  lazy val shouldFollowRES: Boolean = conf.getBoolean("app.shouldFollowRES")
  lazy val whiteList: List[String] = readLines(conf.getString("app.whiteList"))
  lazy val blackList: List[String] = readLines(conf.getString("app.blacklist"))
  def receive = {
    case m: CheckShouldDownload => {
      val s: ActorRef = sender
      val url: String = m.url
      val wbResult: Future[Boolean] = ask(workerPool, WBListCheck(url, whiteList, blackList)).mapTo[Boolean]
      wbResult onSuccess{
        case true =>
          val shouldDownload: Future[Boolean] = for {
            res: Boolean <- if(shouldFollowRES){ask(workerPool, RESCheck(url)).mapTo[Boolean]} else {Future.successful(true)}
            ex: Boolean  <- ask(workerPool, ExistanceCheck(url, m.upRank)).mapTo[Boolean]
            result = !ex && res
          } yield result
          shouldDownload pipeTo s
        case false => Future.successful(false) pipeTo s
      }
    }
    case _ => println("received unknown message")
  }
}
