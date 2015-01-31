package util

import org.apache.lucene.util.Version
import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter
import org.apache.lucene.analysis.ru.RussianLightStemFilter
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, PositionIncrementAttribute}
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer
import org.apache.lucene.morphology.analyzer.MorphologyFilter
import org.apache.lucene.morphology.english.EnglishLuceneMorphology
import org.apache.lucene.morphology.russian.RussianLuceneMorphology

object Term{
  val morphEng = new EnglishLuceneMorphology()
  val morphRu = new RussianLuceneMorphology()
  def extract(content: String): List[String] = {
    val m = scala.collection.mutable.MutableList[String]()
    val tokenizer = new ICUTokenizer(new java.io.BufferedReader(new java.io.StringReader(content)))
    var tokenStream: TokenFilter = new ICUNormalizer2Filter(tokenizer)
    tokenStream = new MorphologyFilter(tokenStream, morphEng)
    tokenStream = new MorphologyFilter(tokenStream, morphRu)
    tokenStream = new RussianLightStemFilter(tokenStream)
    tokenStream = new EnglishMinimalStemFilter(tokenStream)
    val termAttribute = tokenStream.getAttribute(classOf[CharTermAttribute])
    val positionAttribute = tokenStream.getAttribute(classOf[PositionIncrementAttribute])
    tokenStream.reset()
    var tempTermList = scala.collection.mutable.MutableList[String]()
    while (tokenStream.incrementToken()) {
      val term = termAttribute.toString
      val isNew = if(positionAttribute.getPositionIncrement() > 0) true else false
      if(isNew){
        if(!tempTermList.isEmpty){
          m+=tempTermList.sortWith((x,y) => x.length < y.length).head
        }
        tempTermList = scala.collection.mutable.MutableList[String]()
      }
      tempTermList+= term
    }
    if(!tempTermList.isEmpty){
      m+=tempTermList.sortWith((x,y) => x.length < y.length).head
    }
    m.toList
  }
  def index(content: String): Map[String, Set[Int]] = {
    var index = Map[String, Set[Int]]() withDefaultValue Set.empty
    extract(content).zipWithIndex.foreach{
      term => index = index + (term._1 -> (index(term._1) + term._2))
    }
    return index
  }
}
