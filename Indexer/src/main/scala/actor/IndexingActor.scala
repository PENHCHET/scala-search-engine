package actor

import akka.actor.Actor
import util.Term.index
import DAO.{IndexWithURLTable, IndexWithURLRow, IndexWithHashTable, IndexWithHashRow}

class IndexingActor extends Actor{
  def receive = {
    case job: IndexingWithHash => {
      index(job.content).foreach{
        case(term, indices) => IndexWithHashTable.addToIndex(IndexWithHashRow(term, indices.toList, job.hash))
      }
    }
    case job: IndexingWithURL => {
      index(job.content).foreach{
        case(term, indices) => IndexWithURLTable.addToIndex(IndexWithURLRow(term, indices.toList, job.url))
      }
    }
    case kill: DeathWishMessage => {
      println("Kill!")
    }
    case _ => println("Received unknown message")
  }
}
