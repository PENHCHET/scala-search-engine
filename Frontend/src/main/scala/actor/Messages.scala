package actor

sealed trait Message

case class ProcessSearchRequest(query: String, page: Int) extends Message
case class SearchResultEntity(title: String, url: String, aliases: String, cacheId: String, rank: Int) extends Message
case class SearchResults(entities: List[SearchResultEntity], totalNum: Int) extends Message
case class RunQuery(ast: List[List[util.Expr]])

object SearchResultEntity{
  import redis.{ByteStringFormatter, ByteStringSerializer}
  import akka.util.ByteString
  implicit val byteStringFormatter = new ByteStringFormatter[SearchResultEntity]{
    def serialize(data: SearchResultEntity): ByteString = {
      ByteString(data.title + "|" + data.url + "|" + data.aliases + "|" + data.cacheId + "|" + data.rank.toString)
    }
    def deserialize(bs: ByteString): SearchResultEntity = {
        val r = bs.utf8String.split('|').toList
        SearchResultEntity(r(0), r(1), r(2), r(3), r(4).toInt)
    }
  }
}
