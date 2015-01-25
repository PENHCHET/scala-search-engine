package util

sealed trait Expr
sealed trait Operation extends Expr
case class OR(left: Expr, right: Expr) extends Operation
case class AND(left: Expr, right: Expr) extends Operation
case class NOT(op: Expr) extends Operation
sealed trait Terminal extends Expr
case class PhraseTerm(str: String) extends Terminal
case class WordTerm(str: String) extends Terminal
case class ExactTerm(str: String) extends Terminal

object SearchQueryParser{
  import scala.util.{Try, Success, Failure}
  import org.parboiled2._
  sealed class QueryParser(val input: ParserInput) extends Parser {
    val whiteSpaceChar = CharPredicate(" \n\r\t\f")
    implicit def wspStr(s: String): Rule0 = rule{ str(s) ~ zeroOrMore(whiteSpaceChar)}
    def whiteSpace = rule{ zeroOrMore(whiteSpaceChar) }
    def expr = or
    def or = rule{ oneOrMore(and).separatedBy("""|""") ~> (_.reduceLeft(OR)) }
    def and = rule{ oneOrMore(not).separatedBy(whiteSpace) ~> (_.reduceLeft(AND)) | oneOrMore(not).separatedBy("""&""") ~> (_.reduceLeft(AND))}
    def not: Rule1[Expr] = rule{ optional(neg) ~ atom ~> (((a: Option[String], b: Expr) => if (a.isDefined) NOT(b) else b))}
    def neg = rule{ capture(("!")) ~> (_.toString) }
    def atom: Rule1[Expr] = rule{ ((optional(neg) ~ term ~> (((a: Option[String], t: Expr) => if (a.isDefined) NOT(t) else t))) | "(" ~ or ~ (")" | EOI)) }
    def term: Rule1[Terminal] = rule{ notExact | exact | word }
    def notExact: Rule1[Terminal] = rule{ "[" ~ capture(phraseBody) ~ "]" ~ whiteSpace ~> PhraseTerm}
    def exact: Rule1[Terminal] = rule{ "\"" ~ capture(phraseBody) ~ "\"" ~ whiteSpace ~> ExactTerm}
    def phraseBody = rule{ zeroOrMore( noneOf("]\"\\") | ("\\" ~ "\"" ~ "]")) ~ whiteSpace}
    def word: Rule1[Terminal] = rule{ capture(oneOrMore(alphanum)) ~ whiteSpace ~> WordTerm }
    def alphanum = CharPredicate.AlphaNum ++ CharPredicate('\u00c0' to '\u1fff')
  }

  def pSimplifyHelper(ex: Expr): Expr = {
    ex match {
      case NOT(NOT(op)) => op
      case _            => ex
    }
  }

  def pSimplify(ex: Expr): Expr = {
    ex match {
      case NOT(op)   => pSimplifyHelper(NOT(pSimplify(op)))
      case AND(l, r) => pSimplifyHelper(AND(pSimplify(l), pSimplify(r)))
      case OR (l, r) => pSimplifyHelper(OR(pSimplify(l), pSimplify(r)))
      case _         => ex
    }
  }

  def pNNFHelper(ex: Expr): Expr = {
    ex match {
      case AND(l, r)      => AND(pNNFHelper(l), pNNFHelper(r))
      case OR(l, r)       => OR(pNNFHelper(l), pNNFHelper(r))
      case NOT(NOT(op))   => pNNFHelper(op)
      case NOT(AND(l, r)) => OR(pNNFHelper(NOT(l)), pNNFHelper(NOT(r)))
      case NOT(OR(l, r))  => AND(pNNFHelper(NOT(l)), pNNFHelper(NOT(r)))
      case _              => ex
    }
  }

  def pNNF(ex: Expr): Expr = pNNFHelper(pSimplify(ex))

  def pDistribute(s1: List[List[Expr]], s2: List[List[Expr]]) = {for (x ← s1; y ← s2) yield x.union(y)}

  def pDNF(ex: Expr): List[List[Expr]] = {
    val t: List[List[Expr]] = List()
    ex match {
      case AND(l, r) ⇒ pDistribute(pDNF(l), pDNF(r))
      case OR(l, r)  ⇒ pDNF(l).union(pDNF(r))
      case _         ⇒ List(ex) :: t
    }
  }

  def parse(str: String):Try[List[List[Expr]]] = {
    println(str)
    val parser = new QueryParser(str)
    parser.expr.run()
  }.map(pNNF).map(pDNF)

  def serialize(disjunction: List[List[Expr]]) = {
    val baos: java.io.ByteArrayOutputStream = new java.io.ByteArrayOutputStream()
    val oos: java.io.ObjectOutputStream  = new java.io.ObjectOutputStream(baos)
    oos.writeObject(disjunction)
    oos.close();
    baos.toByteArray()
  }
}
