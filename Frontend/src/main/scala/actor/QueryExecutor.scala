package actor

import akka.actor.Actor
import akka.pattern.pipe
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import util.Term.extract
import DAO.{IndexWithURLTable, IndexWithHashTable, HashTable, PageTable, CacheTable}

class QueryExecutor extends Actor{
  def getNotWord(term: String):Future[Set[String]] = if(context.system.settings.config.getBoolean("app.reduceDublicates")){
    IndexWithHashTable.getForNotTerm(term)
  }
  else{IndexWithURLTable.getForNotTerm(term)}
  def getWord(term: String): Future[Set[(String, Set[Int])]] = if(context.system.settings.config.getBoolean("app.reduceDublicates")){
    IndexWithHashTable.getForTerm(term).map{ list => list.map{row => (row.hash.toString, row.indices)}.toSet}
  }
  else{
    IndexWithURLTable.getForTerm(term).map{ list => list.map{row => (row.url, row.indices)}.toSet}
  }
  def getPhrase(parts: Array[String], matching: Option[Set[Int]]): Future[Set[String]] = {
    matching match{
      case Some(m) => if(parts.length > 1){
        getWord(extract(parts.head).head).flatMap{
          setOfPairs: Set[(String, Set[Int])] =>
          val temp = setOfPairs.filter{
            pair: (String, Set[Int]) => (pair._2 & m).isEmpty
          }
          getPhrase(parts.tail, Some(temp.flatMap{_._2.map{_+1}})).map{_ & temp.map{_._1}}
        }
      }
      else{
        getWord(extract(parts.head).head).map{
          setOfPairs: Set[(String, Set[Int])] => setOfPairs.filter{
            pair: (String, Set[Int]) => (pair._2 & m).isEmpty
          }.map{_._1}
        }
      }
      case None => {
        getWord(extract(parts.head).head).flatMap{
          setOfPairs: Set[(String, Set[Int])] => getPhrase(parts.tail, Some(setOfPairs.flatMap{_._2.map{_+1}})).map{_ & setOfPairs.map{_._1} }
        }
      }
    }
  }
  def getAll:Future[Set[String]] = if(context.system.settings.config.getBoolean("app.reduceDublicates")){
    HashTable.getEverything.map{r => r.map{_.hash.toString}.toSet}
  }
  else{
    PageTable.getEverything.map{r => r.map{_.address}.toSet}
  }
  def enforceExact(futureSetOfP: Future[Set[String]], query: String):Future[Set[String]] = {
    futureSetOfP.flatMap{ setOfE =>
      val listOfE = setOfE.toList
      Future.sequence(listOfE.map{
        hashOrUrl: String => if(context.system.settings.config.getBoolean("app.reduceDublicates")){
          HashTable.getCacheIDforHash(java.util.UUID.fromString(hashOrUrl))
        }else{
          PageTable.getCacheIDforUrl(hashOrUrl)
        }}.toList).flatMap{
        listOfCacheID: List[Option[java.util.UUID]] =>
        Future.sequence(listOfCacheID.map{
          case Some(cID) => CacheTable.getForId(cID).map{
            cRow: Option[DAO.CacheRow] =>
            println(cRow)
            cRow match{
              case Some(r) => r.text.getOrElse("")
              case None => ""
            }
          }
          case None => Future("")
        })
      }.map{
        listOfText => println(listOfText)
        (listOfE zip listOfText).filter{ pair => pair._2.indexOf(query) > 0 }.map{_._1}.toSet
      }
    }
  }
  def getResults(exp: util.Expr): Future[Set[String]] = exp match{
    case exp: util.NOT => exp.op match{
      case word: util.WordTerm => val t = getNotWord(extract(word.str).head)
      t. onSuccess{
        case tt: Set[String] => println(s"NotWord set: $tt")
      }
      t
      case phrase: util.PhraseTerm => getPhrase(phrase.str.split("\\s+"), None).flatMap{
        minusSet: Set[String]=> getAll.map{allSet: Set[String] => allSet -- minusSet }
      }
      case exact: util.ExactTerm => {
        enforceExact(getPhrase(exact.str.split("\\s+"), None), exact.str).flatMap{
          minusSet: Set[String]=> getAll.map{allSet: Set[String] => allSet -- minusSet }
        }
      }
      case _ => {
        println("This should not happend")
        Future(Set.empty[String])
        }
    }
    case exp: util.WordTerm => getWord(extract(exp.str).head).map{_.map{_._1}}
    case exp: util.PhraseTerm => getPhrase(exp.str.split("\\s+"), None)
    case exp: util.ExactTerm => enforceExact(getPhrase(exp.str.split("\\s+"), None), exp.str)
    case _ =>  Future(Set.empty[String])
  }
  def receive = {
    case request: RunQuery => {
      val s = sender
      val results: Future[List[String]] = request.ast.foldLeft(Future(Set.empty[String])){
        (resultingSet: Future[Set[String]], orOperand: List[util.Expr]) => {
          resultingSet.flatMap{
            fresulting =>
            orOperand.tail.foldLeft(getResults(orOperand.head)){
              (ands: Future[Set[String]], andOperand: util.Expr) => {
                ands.flatMap{
                  fands => getResults(andOperand).map{_ & fands}
                }
              }
            }.map{_ ++ fresulting}
          }
        }
      }.map(_.toList)
      results onSuccess{
        case t: List[String] => println(t)
      }
      val finalResults: Future[SearchResults] = results.flatMap{
        resultsList: List[String] => if(context.system.settings.config.getBoolean("app.reduceDublicates")){
          Future.sequence(resultsList.map{ hash => HashTable.getHashRowForHash(java.util.UUID.fromString(hash))})
          .map{_.flatMap{x=>x}}
          .flatMap{listOfHr => Future.sequence(listOfHr.map{ hr => {
            Future.sequence(hr.pages.toList.map{address => PageTable.getPageForAddress(address)})
            .map{_.flatMap{x=>x}}
            .map{listOfPages =>
              val sorted = listOfPages.sortWith(_.rank > _.rank)
              SearchResultEntity(
                sorted.head.title.getOrElse(sorted.head.address),
                sorted.head.address,
                sorted.tail.map(_.address).mkString(","),
                sorted.head.cacheID.map(_.toString).getOrElse(""),
                sorted.head.rank)
            }
          }})}
        }
        else{
          println("URL")
          Future.sequence(resultsList.map{url: String => PageTable.getPageForAddress(url)})
          .map{_.flatMap{x=>x}}
          .map{_.map{ pageRow =>
              SearchResultEntity(
                pageRow.title.getOrElse(pageRow.address),
                pageRow.address,
                "",
                pageRow.cacheID.map(_.toString).getOrElse(""),
                pageRow.rank)
          }}
        }
      }.map{pages: List[SearchResultEntity]=> SearchResults(pages.sortWith(_.rank > _.rank), pages.length)}
      finalResults pipeTo s
    }
  }
}
