package actor

import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.Actor

import spray.routing.HttpService
import spray.http.{HttpRequest, HttpResponse, Timedout, StatusCodes}
import spray.http.ContentTypes._
import spray.http.HttpHeaders._
//
import spray.json._
import spray.httpx.SprayJsonSupport._
import util.JSONProtocols._
import util.JSONProtocols.SearchRequestJsonProtocol._
import util.JSONProtocols.SearchResultsJsonProtocol._

trait SEFrontendService extends HttpService{
  implicit val timeout = Timeout(60.second)
  val static =
  get{
    path(""){
      getFromResource("www/index.html")
    } ~ {
      getFromResourceDirectory("www")
    }
  }
  val api =
  post{
    path("api"/"search"){
      entity(as[SearchRequestJson]){ request =>
        onComplete(ask(actorRefFactory.actorSelection("akka://actor-system/user/se-manager"), ProcessSearchRequest(request.query, request.page)).mapTo[SearchResults]){
          case Success(result) => complete(result.toJson.compactPrint.stripMargin)
          case Failure(ex) => complete(s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }
}

class SEServiceActor extends Actor with SEFrontendService{
  def actorRefFactory = context
  def receive = handleTimeouts orElse runRoute(api ~ static)
  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
    sender ! HttpResponse(StatusCodes.InternalServerError, "Request timed out.")
  }
}
