package actor

import akka.actor.Actor
import util.Term.index
import DAO.{IndexWithURLTable, IndexWithURLRow, IndexWithHashTable, IndexWithHashRow}

class IndexingActor extends Actor{
  def receive = {
    case job: IndexingWithHash => {
      println(job.hash)
      index(job.content).foreach{
        case(term, indexes) => IndexWithHashTable.addToIndex(IndexWithHashRow(term, indexes.toList, job.hash))
      }
    }
    case job: IndexingWithURL => {
      println(job.url)
      index(job.content).foreach{
        case(term, indexes) => IndexWithURLTable.addToIndex(IndexWithURLRow(term, indexes.toList, job.url))
      }
    }
    case kill: DeathWishMessage => {
      println("Kill!")
    }
    case _ => println("Received unknown message")
  }
}
