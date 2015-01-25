package util

object FileReader
{
  def fileContentLineByLineList(filePath:String): List[String] = try{
    val seedSource = scala.io.Source.fromURL(getClass.getResource(filePath))
    val seedList  = seedSource.getLines().toList
    seedSource.close()
    seedList
  }catch{
    case e:Exception => List()
  }
}
