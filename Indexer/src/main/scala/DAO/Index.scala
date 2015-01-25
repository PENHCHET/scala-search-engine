package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class IndexWithHashRow(term: String, indexes: List[Int], hash: java.util.UUID)
case class IndexWithURLRow(term: String, indexes: List[Int], url: String)

sealed class IndexWithHashTable extends CassandraTable[IndexWithHashTable, IndexWithHashRow]{
  object term extends StringColumn(this) with PartitionKey[String]
  object indexes extends ListColumn[IndexWithHashTable, IndexWithHashRow, Int](this)
  object hash extends UUIDColumn(this) with PrimaryKey[java.util.UUID]
  def fromRow(r: Row): IndexWithHashRow = {
    IndexWithHashRow(
      term(r),
      indexes(r),
      hash(r)
    )
  }
}

sealed class IndexWithURLTable extends CassandraTable[IndexWithURLTable, IndexWithURLRow]{
  object term extends StringColumn(this) with PartitionKey[String]
  object indexes extends ListColumn[IndexWithURLTable, IndexWithURLRow, Int](this)
  object url extends StringColumn(this) with PrimaryKey[String]
  def fromRow(r: Row): IndexWithURLRow = {
    IndexWithURLRow(
      term(r),
      indexes(r),
      url(r)
    )
  }
}

object IndexWithHashTable extends IndexWithHashTable with SearchEngineConnector{
  override lazy val tableName = "Inverted_Index"
  val s: Session = session
  s.execute("USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(IndexWithHashTable.create.future(), 5000 millis)
  }
  def addToIndex(r: IndexWithHashRow): ScalaFuture[ResultSet] = {
    insert.value(_.term, r.term)
    .value(_.indexes, r.indexes)
    .value(_.hash, r.hash)
    .future()
  }
}

object IndexWithURLTable extends IndexWithURLTable with SearchEngineConnector{
  override lazy val tableName = "Inverted_Index"
  val s: Session = session
  s.execute("USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(IndexWithURLTable.create.future(), 5000 millis)
  }
  def addToIndex(r: IndexWithURLRow): ScalaFuture[ResultSet] = {
    insert.value(_.term, r.term)
    .value(_.indexes, r.indexes)
    .value(_.url, r.url)
    .future()
  }
}
