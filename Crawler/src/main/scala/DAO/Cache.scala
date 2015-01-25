package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class CacheRow(
  id: java.util.UUID,
  content_Type: Option[String],
  text: Option[String],
  data: java.nio.ByteBuffer
)

sealed class CacheTable extends CassandraTable[CacheTable, CacheRow]{
  object id extends UUIDColumn(this) with PartitionKey[java.util.UUID]
  object content_Type extends OptionalStringColumn(this)
  object text extends OptionalStringColumn(this)
  object data extends BlobColumn(this)
  def fromRow(r: Row): CacheRow = {
    CacheRow(
      id(r),
      content_Type(r),
      text(r),
      data(r)
    )
  }
}

object CacheTable extends CacheTable with SearchEngineConnector{
  override lazy val tableName = "Cache"
  val s: Session = session
  s.execute(s"USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(CacheTable.create.future(), 5000 millis)
  }
  def addNew(r: CacheRow) : ScalaFuture[ResultSet] = {
    insert.value(_.id, r.id)
      .value(_.content_Type, r.content_Type)
      .value(_.text, r.text)
      .value(_.data, r.data)
      .future()
  }
  def updateTextForCache(uuid: java.util.UUID, text: Option[String]): ScalaFuture[ResultSet] =  {
    update.where(_.id eqs uuid).modify(_.text setTo text).future()
  }
}
