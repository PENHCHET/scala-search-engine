package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class IndexWithHashRow(term: String, indices: Set[Int], hash: java.util.UUID)
case class IndexWithURLRow(term: String, indices: Set[Int], url: String)

sealed class IndexWithHashTable extends CassandraTable[IndexWithHashTable, IndexWithHashRow]{
  object term extends StringColumn(this) with PartitionKey[String]
  object indices extends SetColumn[IndexWithHashTable, IndexWithHashRow, Int](this)
  object hash extends UUIDColumn(this) with PrimaryKey[java.util.UUID]
  def fromRow(r: Row): IndexWithHashRow = {
    IndexWithHashRow(
      term(r),
      indices(r),
      hash(r)
    )
  }
}

sealed class IndexWithURLTable extends CassandraTable[IndexWithURLTable, IndexWithURLRow]{
  object term extends StringColumn(this) with PartitionKey[String]
  object indices extends SetColumn[IndexWithURLTable, IndexWithURLRow, Int](this)
  object url extends StringColumn(this) with PrimaryKey[String]
  def fromRow(r: Row): IndexWithURLRow = {
    IndexWithURLRow(
      term(r),
      indices(r),
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
  def getForTerm(term: String) = {
    IndexWithHashTable.select.where(_.term eqs term).fetch
  }
  def getForNotTerm(term: String) = {
    IndexWithHashTable.select.fetch.flatMap{
      seq =>
      IndexWithHashTable.select.where(_.term eqs term).fetch.map{
        seq2 =>
        seq.map{_.hash.toString}.toSet &~ seq2.map{_.hash.toString}.toSet
      }
    }
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
  def getForTerm(term: String) = {
    IndexWithURLTable.select.where(_.term eqs term).fetch
  }

  def getForNotTerm(term: String) = {
    IndexWithURLTable.select.fetch.flatMap{
      seq => IndexWithURLTable.select.where(_.term eqs term).fetch.map{
        seq2 => seq.map{_.url}.toSet &~ seq2.map{_.url}.toSet
      }
    }
  }
}
