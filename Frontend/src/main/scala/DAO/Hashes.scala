package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class HashRow(
  hash: java.util.UUID,
  pages: Set[String],
  cacheID: Option[java.util.UUID]
)

sealed class HashTable extends CassandraTable[HashTable, HashRow]{
  object hash extends UUIDColumn(this) with PartitionKey[java.util.UUID]
  object pages extends SetColumn[HashTable, HashRow, String](this)
  object cacheID extends OptionalUUIDColumn(this)
  def fromRow(r: Row): HashRow = {
    HashRow(
      hash(r),
      pages(r),
      cacheID(r)
    )
  }
}

object HashTable extends HashTable with SearchEngineConnector{
  override lazy val tableName = "Hashes"
  val s: Session = session
  s.execute(s"USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(HashTable.create.future(), 5000 millis)
  }

  def getCacheIDforHash(hash: java.util.UUID): ScalaFuture[Option[java.util.UUID]] = {
    select.where(_.hash eqs hash).one().map{
      case Some(result) => result.cacheID
      case None => None
    }
  }

  def getHashRowForHash(hash: java.util.UUID): ScalaFuture[Option[HashRow]] = select.where(_.hash eqs hash).one()

  def getEverything: ScalaFuture[Seq[HashRow]] = select.fetch
}
