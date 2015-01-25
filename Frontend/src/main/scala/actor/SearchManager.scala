package actor

import akka.actor.{Actor, Props}
import akka.pattern.{ask,pipe}
import scala.concurrent.Future
import redis.RedisClient
import scala.concurrent.ExecutionContext.Implicits.global
import util.SearchQueryParser
import scala.util.{Try, Success, Failure}
import akka.util.Timeout
import scala.concurrent.duration._

class SearchManagerActor extends Actor{
  implicit val timeout = Timeout(20.second)
  implicit val akkaSystem = context.system
  val redis = RedisClient()
  val queryExecuter = context.actorOf(Props[QueryExecutor], "QueryExecutor")
  def receive = {
    case request: ProcessSearchRequest => {
      val s = sender
      val queryAST = SearchQueryParser.parse(request.query)
      println(queryAST)
      queryAST match{
        case Success(ast) => {
          val searchQueryUUID = java.util.UUID.nameUUIDFromBytes(SearchQueryParser.serialize(ast))
          val startingFrom = 10*(request.page-1)
          val result: Future[SearchResults] = redis.exists(searchQueryUUID.toString + "length").flatMap{
            exists: Boolean =>
            if(exists){
              println("Exists")
              for{
                length <- redis.get[String](searchQueryUUID.toString + "length")
                results <- redis.lrange[SearchResultEntity](searchQueryUUID.toString, startingFrom, startingFrom + 9)
              } yield SearchResults(results.toList, length.get.toInt)
            }
            else{
              println("Not Exists")
              ask(queryExecuter, RunQuery(ast)).mapTo[SearchResults].map{
                sr: SearchResults =>
                redis.set(searchQueryUUID.toString + "length", sr.totalNum)
                println(sr.totalNum)
                sr.entities.foreach{ent: SearchResultEntity => redis.rpush(searchQueryUUID.toString, ent)}
                SearchResults(sr.entities.take(10), sr.totalNum)
              }
            }
          }
          result pipeTo s
        }
        case Failure(err) => Future(err) pipeTo s
      }
    }
    case _ => println("Received unknown message")
  }
}
