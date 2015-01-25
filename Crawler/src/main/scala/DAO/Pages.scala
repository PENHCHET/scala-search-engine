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
  def addNewPage(r: PageRow) : ScalaFuture[ResultSet] = {
    insert.value(_.address, r.address)
      .value(_.title, r.title)
      .value(_.rank, r.rank)
      .value(_.cacheID, r.cacheID)
      .future()
  }
  def checkExistance(url: String): ScalaFuture[Boolean] = {
    select.where(_.address eqs url).one().map{
      case Some(page) => true
      case None => false
    }
  }
  def updateTitleOfPage(url: String, title: Option[String]): ScalaFuture[ResultSet] =  {
    update.where(_.address eqs url).modify(_.title setTo title).future()
  }
  def increasePageRank(url: String): Unit = {
    println(s"Increasing pRank of $url")
    select.where(_.address eqs url).one().collect{
      case Some(page)=>
        val r = page.rank
        update.where(_.address eqs url).modify(_.rank setTo r+1).future()
      case None => None
    }
  }
  def updateCacheID(url: String, id: java.util.UUID):  ScalaFuture[ResultSet] =  {
    update.where(_.address eqs url).modify(_.cacheID setTo Some(id)).future()
  }
}
