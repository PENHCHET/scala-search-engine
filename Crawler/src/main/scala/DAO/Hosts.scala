package DAO

import scala.concurrent.{Future => ScalaFuture}
import com.websudos.phantom.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

case class HostRow(
  host_address: String,
  crawlDelay: Option[Int],
  lastRequestTime: Option[org.joda.time.DateTime],
  robotsAllowList: List[String],
  robotsDisallowList: List[String]
)

sealed class HostTable extends CassandraTable[HostTable, HostRow]{
  object host_address extends StringColumn(this) with PartitionKey[String]
  object crawlDelay extends OptionalIntColumn(this)
  object lastRequestTime extends OptionalDateTimeColumn(this)
  object robotsAllowList extends ListColumn[HostTable, HostRow, String](this)
  object robotsDisallowList extends ListColumn[HostTable, HostRow, String](this)
  def fromRow(r: Row): HostRow = {
    HostRow(
      host_address(r),
      crawlDelay(r),
      lastRequestTime(r),
      robotsAllowList(r),
      robotsDisallowList(r)
    )
  }
}

object HostTable extends HostTable with SearchEngineConnector{
  override lazy val tableName = "Hosts"
  val s: Session = session
  s.execute(s"USE system;")
  val result = s.execute(s"SELECT columnfamily_name FROM schema_columnfamilies WHERE keyspace_name='$keySpace';").all()
  s.execute(s"USE $keySpace;")
  if(!result.contains(tableName.toLowerCase)){
    Await.result(HostTable.create.future(), 5000 millis)
  }

  def getHost(url: String): ScalaFuture[Option[HostRow]] = {
    select.where(_.host_address eqs url).one()
  }
  def addNewHost(host: HostRow): ScalaFuture[ResultSet] = {
    insert.value(_.host_address, host.host_address)
    .value(_.crawlDelay, host.crawlDelay)
    .value(_.lastRequestTime, host.lastRequestTime)
    .value(_.robotsAllowList, host.robotsAllowList)
    .value(_.robotsDisallowList, host.robotsDisallowList)
    .future()
  }
  def updateLRTofHost(url: String): ScalaFuture[ResultSet] = {
    update.where(_.host_address eqs url).modify(_.lastRequestTime setTo Some(org.joda.time.DateTime.now())).future()
  }
}
