package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class PageRow(
  address: String,
  title: Option[String],
  rank: Int,
  cacheID: Option[java.util.UUID]
)

sealed class PageTable extends CassandraTable[PageTable, PageRow]{
  object address extends StringColumn(this) with PartitionKey[String]
  object title extends OptionalStringColumn(this)
  object rank extends IntColumn(this)
  object cacheID extends OptionalUUIDColumn(this)
  def fromRow(r: Row): PageRow = {
    PageRow(
      address(r),
      title(r),
      rank(r),
      cacheID(r)
    )
  }
}

object PageTable extends PageTable with SearchEngineConnector{
  override lazy val tableName = "Pages"
  val s: Session = session
  s.execute("USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(PageTable.create.future(), 5000 millis)
  }
  def getEverything = select.fetch

  def getCacheIDforUrl(url: String): ScalaFuture[Option[java.util.UUID]] = {
    select.where(_.address eqs url).one().map{
      case Some(result) => result.cacheID
      case None => None
    }
  }
  def getPageForAddress(url: String) = select.where(_.address eqs url).one()
}
