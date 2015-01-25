package DAO

import com.websudos.phantom.zookeeper.{DefaultZookeeperConnector, SimpleCassandraConnector}

trait SearchEngineConnector extends SimpleCassandraConnector {
  val keySpace = "search_engine"
}

/**
* Now you might ask yourself how to use service discovery with phantom. The Datastax Java Driver can automatically connect to multiple clusters.
* Using some underlying magic, phantom can also help you painlessly connect to a series of nodes in a Cassandra cluster via ZooKeeper.
*
* Once again, all you need to tell phantom is what your keyspace is. Phantom will make a series of assumptions about which path you are using in ZooKeeper.
* By default, it will try to connect to localhost:2181, fetch the "/cassandra" path and parse ports found in a "host:port, host1:port1,
* .." sequence. All these settings are trivial to override in the below connector and you can adjust all the settings to fit your environment.
*/
trait SearchEngineZooKeeperConnector extends DefaultZookeeperConnector {
  val keySpace = "search_engine"
}
