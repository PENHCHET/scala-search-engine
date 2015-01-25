package actor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{pipe, ask}
import akka.util.Timeout

import spray.http.HttpResponse

import DAO.{PageTable, HostTable, HostRow}

class FrontierGuard extends Actor{
  implicit val timeout = Timeout(60.second)
  val downloader = context.actorOf(Props[NetworkActor], name = "FrontierGuardsDownloader")
  def checkL(url:String, l:List[String]):Boolean = {
    object ConformsToSome extends Exception
    object ConformsToNone extends Exception
    try{
      for (regex <- l){
        if(url.matches(regex)) throw ConformsToSome
      }
      throw ConformsToNone
    }
    catch {
      case ConformsToSome => true
      case ConformsToNone => false
    }
  }
  def receive = {
    case m: WBListCheck => {
      val s: ActorRef = sender
      if(!m.wl.isEmpty){
        val result = checkL(m.url, m.wl)
        Future(result) pipeTo s
      }
      else{
        if(!m.bl.isEmpty){
          val result = !checkL(m.url, m.bl)
          Future(result) pipeTo s
        }
        else{
          Future(true) pipeTo s
        }
      }
    }
    case m: ExistanceCheck => {
      val s: ActorRef = sender
      val ex = PageTable.checkExistance(m.url)
      ex.collect{
        case true => if(m.upRank){PageTable.increasePageRank(m.url)}
        case false => None
      }
      ex pipeTo s
    }
    case m: RESCheck => {
      val s: ActorRef = sender
      val uri = new java.net.URI(m.url)
      val hostAddress = uri.getScheme() + "://" + uri.getAuthority();
      val hostRow = HostTable.getHost(hostAddress)
      val regexTuple: Future[(List[String], List[String])] = hostRow.flatMap{
        host => host match{
          case Some(host) =>
            Future((host.robotsDisallowList, host.robotsAllowList))
          case None => {
            val resp = ask(downloader, DownloadAndReturn(uri.getScheme() + "://" + uri.getAuthority() + "/robots.txt")).mapTo[HttpResponse]
            resp map {
              resp: HttpResponse => {
                if(resp.status.isSuccess){
                  val robotsContent: String = resp.entity.asString
                  def getAllRules(content: String): List[String] = {
                    val temp = content.toUpperCase
                    val indexOfDefaultUserAgent = temp.indexOf("User-Agent: *".toUpperCase)
                    val endOfDefaultUserAgentDerective = temp.indexOf("User-Agent:".toUpperCase, indexOfDefaultUserAgent + "User-Agent: *".length)
                    if(indexOfDefaultUserAgent < 0){List()}
                    else{
                      if(endOfDefaultUserAgentDerective < 0){List(content.substring(indexOfDefaultUserAgent, content.length))}
                      else{content.substring(indexOfDefaultUserAgent, endOfDefaultUserAgentDerective)::getAllRules(content.drop(endOfDefaultUserAgentDerective))}
                    }
                  }
                  val robotsRules:List[String] = getAllRules(robotsContent).flatMap(g => g.split("\n")).map(t => t.toLowerCase)
                  val allowList = robotsRules collect {case line:String if line.startsWith("Allow:") => line.drop("Allow:".length).trim}
                  val disAllowList = robotsRules collect {case line:String if line.startsWith("Disallow:") => line.drop("Disallow:".length).trim}
                  val crawlDelay : Option[Int] = {
                    if(context.system.settings.config.getBoolean("app.shouldDelayCrawl")){
                      val robotsCDList = robotsRules collect {case line:String if line.startsWith("crawl-delay:") => line.drop("crawl-delay:".length).trim.toFloat.toInt}
                      def max[A <% Ordered[A]](xs: Seq[A]): Option[A] = xs match {
                        case s if s.isEmpty || !s.hasDefiniteSize => None
                        case s if s.size == 1 => Some(s(0))
                        case s if s(0) <= s(1) => max(s drop 1)
                        case s => max((s drop 1).updated(0, s(0)))
                      }
                      val maxCD = max(robotsCDList)
                      maxCD match{
                        case cd: Some[Int] => cd
                        case None => Some(context.system.settings.config.getInt("app.defaultDelay"))
                      }
                    }else {None}
                  }
                  val cleanAList = allowList.filterNot(_ == "")
                  val cleanDAList = disAllowList.filterNot(_ == "")
                  val allowRegexList = (if(cleanDAList.length < disAllowList.length){"/"::cleanAList} else {cleanAList}) map{path => """^.*(""" + path.replaceAll("""\.""", """\\.""").replaceAll("""/""", """\\/""").replaceAll("""\*""", """.*""") + """).*$"""}
                  val disAllowRegexList = (if(cleanAList.length < allowList.length){"/"::cleanDAList} else {cleanDAList}) map{path => """^.*(""" + path.replaceAll("""\.""", """\\.""").replaceAll("""/""", """\\/""").replaceAll("""\*""", """.*""") + """).*$"""}
                  val now = crawlDelay match{
                    case Some(crawlDelay) => Some(org.joda.time.DateTime.now())
                    case None => None
                  }
                  HostTable.addNewHost(HostRow(
                    hostAddress,
                    crawlDelay,
                    now,
                    allowRegexList,
                    disAllowRegexList
                  ))
                  (disAllowRegexList, allowRegexList)
                }
                else{
                  val crawlDelay : Option[Int] = {
                    if(context.system.settings.config.getBoolean("app.shouldDelayCrawl")){
                      Some(context.system.settings.config.getInt("app.defaultDelay"))
                    }else{None}
                  }
                  val now = crawlDelay match{
                    case Some(crawlDelay) => Some(org.joda.time.DateTime.now())
                    case None => None
                  }
                  HostTable.addNewHost(HostRow(
                    hostAddress,
                    if(context.system.settings.config.getBoolean("app.shouldDelayCrawl")){Some(context.system.settings.config.getInt("app.defaultDelay"))} else{None},
                    now,
                    List(""".*"""),
                    List()
                  ))
                  (List(), List(""".*"""))
                }
              }
            }
          }
        }
      }
      val result: Future[Boolean] = regexTuple map{regex:(List[String], List[String]) => checkL(m.url , regex._2) || !checkL(m.url , regex._1)}
      result pipeTo s
    }
    case _ => println("received unknown message")
  }
}
