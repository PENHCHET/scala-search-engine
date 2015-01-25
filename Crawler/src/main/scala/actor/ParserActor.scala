package actor

import akka.actor.Actor

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import DAO.{PageTable, CacheTable}

class ParserActor extends Actor{
  def receive = {
    case resource: ParseResource => {
      if(resource.newToIndex){
        resource.cType match{
          case "text/html" => {
            val allLinks: scala.collection.mutable.Set[String] = scala.collection.mutable.Set()
            val doc: Document = Jsoup.parse(new String(resource.data, java.nio.charset.Charset.forName(resource.charset.getOrElse("UTF-8"))), resource.url)
            if(context.system.settings.config.getBoolean("app.shouldFollowMedia")){
              val media: Elements = doc.select("[src]")
              media.iterator.foreach{
                el: Element => {
                  val uri = new java.net.URI(el.attr("abs:src")).normalize()
                  val link = if(uri.isAbsolute()) uri.toString() else uri.resolve(resource.url).toString()
                  allLinks += link
                }
              }
            }
            if(context.system.settings.config.getBoolean("app.shouldFollowImports")){
              val imports: Elements = doc.select("link[href]")
              imports.iterator.foreach{
                el: Element => {
                  val uri = new java.net.URI(el.attr("abs:href")).normalize()
                  val link = if(uri.isAbsolute()) uri.toString() else uri.resolve(resource.url).toString()
                  allLinks += link
                }
              }
            }
            val links: Elements = doc.select("a[href]");
            links.iterator.foreach{
              el: Element => {
                val uri = new java.net.URI(el.attr("abs:href")).normalize()
                val link = if(uri.isAbsolute()) uri.toString() else uri.resolve(resource.url).toString()
                allLinks += link
              }
            }

            PageTable.updateTitleOfPage(resource.url, Some(doc.select("title").text().trim()))

            for{
              cID <- resource.cacheID
              cIDVal <- cID.map(scala.concurrent.Future.successful).getOrElse(scala.concurrent.Future.failed(new Exception ))
            } yield CacheTable.updateTextForCache(cIDVal, Some(doc.text().trim()))

            val rA = context.actorSelection("/user/RootActor")
            allLinks.foreach{
              link => {
                val upRank = if(new java.net.URI(link).getAuthority() != new java.net.URI(resource.url).getAuthority()) true else false
                rA ! NewLink(link.split("#").head, resource.depth + 1, upRank)
              }
            }
            val msg = if(context.system.settings.config.getBoolean("app.reduceDublicates") && !resource.hash.isEmpty){
              IndexingWithHash(resource.hash.get, doc.text().trim())
            }
            else{
              IndexingWithURL(resource.url, doc.text().trim())
            }
            context.actorSelection("akka.tcp://IndexerSystem@127.0.0.1:2553/user/IndexingPool") ! msg
          }
          case "text/xml" => println("ToDo: text/xml")
          case "application/xhtml+xml" => println("ToDo: application/xhtml+xml")
          case "application/xml" => println("ToDo: application/xml")
          case "application/rss+xml" => println("ToDo: application/rss+xml")
          case "application/json" => println("ToDo: application/json")
          case _ => println("Content-type not supported")
        }
      }
    }
    case _ => println("received unknown message")
  }
}
