package actor

sealed trait Message

case class IndexingWithURL(url: String, content: String) extends Message
case class IndexingWithHash(hash: java.util.UUID, content: String) extends Message
case class DeathWishMessage() extends Message
