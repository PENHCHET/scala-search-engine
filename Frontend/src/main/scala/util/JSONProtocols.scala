package util.JSONProtocols

import spray.json._
import actor.{SearchResultEntity, SearchResults}

case class SearchRequestJson(query: String, page: Int)

object SearchRequestJsonProtocol extends DefaultJsonProtocol{
  implicit val searchRequestJsonFormat = jsonFormat2(SearchRequestJson)
}

object SearchResultsJsonProtocol extends DefaultJsonProtocol{
  implicit val SearchResultEntityJsonFormat: RootJsonFormat[SearchResultEntity] = jsonFormat(SearchResultEntity.apply, "title", "url", "aliases", "cacheId", "rank")
  implicit def SearchResultsJsonFormat = jsonFormat2(SearchResults.apply)
}
