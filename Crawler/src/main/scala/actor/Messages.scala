package actor

import spray.http.{HttpResponse, HttpRequest}
sealed trait Message

case class CheckShouldDownload(url: String, upRank: Boolean) extends Message
case class WBListCheck(url: String, wl: List[String], bl: List[String]) extends Message
case class RESCheck(url: String) extends Message
case class ExistanceCheck (url: String, upRank: Boolean) extends Message
case class DownloadAndReturn(url: String) extends Message
case class DoCrawlJob(url: String, depth: Int, upRank: Boolean) extends Message
case class MakeHTTPRequest(request: HttpRequest) extends Message
case class ProcessResponse(url: String, response: HttpResponse, depth: Int, upRank: Boolean) extends Message
case class ParseResource(url: String, hash: Option[java.util.UUID], cacheID: scala.concurrent.Future[Option[java.util.UUID]], data: Array[Byte], cType: String, charset: Option[String], depth: Int, newToIndex: Boolean) extends Message
case class NewLink(url: String, depth: Int, upRank: Boolean) extends Message
case class IndexingWithURL(url: String, content: String) extends Message
case class IndexingWithHash(hash: java.util.UUID, content: String) extends Message
case class DeathWishMessage() extends Message
