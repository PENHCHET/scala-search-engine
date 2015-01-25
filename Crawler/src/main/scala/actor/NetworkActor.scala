package actor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import akka.pattern.{ask,pipe}
import akka.util.Timeout

import spray.http._
import spray.client.pipelining.sendReceive
import DAO.{HostTable, HostRow}

class NetworkActor extends Actor{
  implicit val timeout = Timeout(10.second)
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  val tSettings = context.system.settings.config.getBoolean("app.shouldDelayCrawl")
  def receive = {
    case message: MakeHTTPRequest =>
      val s = sender
      val request: HttpRequest = message.request
      request.method.value match {
        case "GET" => pipeline{request} pipeTo s
        case v: String => println(s"Non-GET method invocation: $v")
      }
    case task: DownloadAndReturn =>
      val s = sender
      val uri = new java.net.URI(task.url)
      ask(self, MakeHTTPRequest(HttpRequest(uri = uri.getScheme() + "://" + java.net.IDN.toASCII(uri.getAuthority()) + uri.getPath()))) pipeTo s
    case job: DoCrawlJob =>
      if(tSettings){
        val uri = new java.net.URI(job.url)
        val hostAddress = uri.getScheme() + "://" + uri.getAuthority()
        HostTable.getHost(hostAddress) onSuccess{
          hr: Option[HostRow] => hr match{
            case Some(hr) =>
            if(org.joda.time.DateTime.now().minusSeconds(hr.crawlDelay.get).isBefore(hr.lastRequestTime.get)){
              self ! job
            }
            else{
              ask(self, DownloadAndReturn(job.url)).mapTo[HttpResponse] onSuccess{
                case response: HttpResponse =>
                  HostTable.updateLRTofHost(hostAddress)
                  if(response.status.isSuccess){
                    context.actorSelection("/user/RootActor/PageProcessingManager") ! ProcessResponse(job.url, response, job.depth, job.upRank)
                  }
              }
            }
          }
        }
      }
      else{
        ask(self, DownloadAndReturn(job.url)).mapTo[HttpResponse] onSuccess{
          case response: HttpResponse => {
            if(response.status.isSuccess){
              context.system.actorSelection("/user/RootActor/PageProcessingManager") ! ProcessResponse(job.url, response, job.depth, job.upRank)
            }
          }
        }
      }
    case _ => println("received unknown message")
  }
}
