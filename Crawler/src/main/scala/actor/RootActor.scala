package actor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{RoundRobinPool, DefaultResizer}
import akka.pattern.ask
import akka.util.Timeout

import spray.http.HttpResponse

class RootActor extends Actor{
  implicit val timeout = Timeout(60.second)
  val resizer = DefaultResizer(lowerBound = 1, upperBound = 10)
  val fManager = context.actorOf(Props[FrontierManager], "FrontierManager")
  val cManager = context.actorOf(RoundRobinPool(2, Some(resizer)).props(Props[NetworkActor]), "CrawlManager")
  val ppManager = context.actorOf(Props[PageProcessingManager], "PageProcessingManager")
  def receive = {
    case r: NewLink =>
      val mDepth = context.system.settings.config.getInt("app.maxDepth")
      if(mDepth < 0 || (mDepth > 0 && mDepth >= r.depth)){
        val shouldDownload = ask(fManager, CheckShouldDownload(r.url, r.upRank)).mapTo[Boolean]
        shouldDownload onComplete {
          case Success(sD)  => {
            if(sD){cManager ! DoCrawlJob(r.url, r.depth, r.upRank)}
          }
          case Failure(e) => println("Failure: " + e.getMessage)
        }
      }
    case kill: DeathWishMessage => println("Kill!")
    case _ => println("received unknown message")
  }
}
