package actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.routing.{RoundRobinPool, DefaultResizer, Resizer}

import scala.concurrent.ExecutionContext.Implicits.global

import DAO.{PageTable, PageRow, HashTable, HashRow, CacheTable, CacheRow}

class PageProcessingManager extends Actor{
  val resizer: Resizer = DefaultResizer(lowerBound = 1, upperBound = 10)
  val parserPool = context.actorOf(RoundRobinPool(5, Some(resizer)).props(Props[ParserActor]), "ParserPool")
  def generateAndWriteCacheID(data: Option[java.nio.ByteBuffer], contentType: Option[String]): Option[java.util.UUID] = if(context.system.settings.config.getBoolean("app.cacheFiles")){
    val uuid = java.util.UUID.randomUUID()
    CacheTable.addNew(CacheRow(uuid, contentType, None, data.get))
    Some(uuid)
  }else{
    None
  }
  def receive = {
    case m: ProcessResponse => {
      val written = PageTable.checkExistance(m.url)
      written onSuccess{
        case false => {
          val cType = m.response.headers.find{h => h is("content-type")} match{ case Some(h) => Some(h.value); case None => None}
          cType match{
            case Some(cType) => {
              //println(s"${m.url} ${m.upRank}")
              PageTable.addNewPage(PageRow(m.url, None, if(m.upRank) 1 else 0, None))
              val cleanContentType = ("""(^\S*\/[^\s\;]*)""".r findFirstIn(cType))
              val charset = (for (m <- """charset\s*?=\s*?([^"'\s;]+)""".r findAllMatchIn cType) yield m group 1).toList.headOption
              val dataBlob: Option[java.nio.ByteBuffer] = if(context.system.settings.config.getBoolean("app.cacheFiles")) Some(java.nio.ByteBuffer.wrap(m.response.entity.data.toByteArray)) else None
              val cacheID: scala.concurrent.Future[Option[java.util.UUID]] = if(context.system.settings.config.getBoolean("app.reduceDublicates")){
                val hash = java.util.UUID.nameUUIDFromBytes(m.response.entity.data.toByteArray)
                val cacheID: scala.concurrent.Future[Option[java.util.UUID]] = HashTable.checkExistance(hash).flatMap{
                  case true => {
                    HashTable.addPageToExisting(hash, m.url)
                    val cID = HashTable.getCacheIDforHash(hash)
                    parserPool ! ParseResource(m.url, Some(hash), cID, m.response.entity.data.toByteArray, cleanContentType.get, charset, m.depth, false)
                    cID
                  }
                  case false => {
                    val cacheID = generateAndWriteCacheID(dataBlob, cleanContentType)
                    parserPool ! ParseResource(m.url, Some(hash), scala.concurrent.Future(cacheID), m.response.entity.data.toByteArray, cleanContentType.get, charset, m.depth, true)
                    HashTable.addNewHash(HashRow(hash, Set(m.url), cacheID))
                    scala.concurrent.Future(cacheID)
                  }
                }
                cacheID
              }
              else{
                val cID = scala.concurrent.Future(generateAndWriteCacheID(dataBlob, cleanContentType))
                parserPool ! ParseResource(m.url, None, cID, m.response.entity.data.toByteArray, cleanContentType.get, charset, m.depth, true)
                cID
              }
              cacheID collect {
                case Some(cID) => PageTable.updateCacheID(m.url, cID)
                case None => None
              }
            }
            case None => println("Unknown content-type")
          }
        }
      }
    }
    case _ => println("received unknown message")
  }
}
