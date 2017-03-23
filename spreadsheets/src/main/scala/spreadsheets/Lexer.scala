package spreadsheets

import scala.annotation.switch
import scala.util.matching.Regex

object Lexer {
  sealed abstract class Token
  case class TSimple(str: String) extends Token
  case class TOp(op: String, precedence: Int, leftAssociative: Boolean = true) extends Token
  case class TFunc(func: String) extends Token
  case class TCellRef(col: Int, row: Int) extends Token
  case class TStrLit(str: String) extends Token
  case class TNumLit(num: Double) extends Token

  val TOpenPar = TSimple("(")
  val TClosePar = TSimple(")")
  val EOF = TSimple("<EOF>")
  val TEmptyTuple = TSimple("{}")
  val TSemicolon = TOp(";", 0)  // semicolon treated as a binary operation that unify two values into a tuple
  val TEq = TOp("=", 1)
  val TNeq = TOp("<>", 1)
  val TGe = TOp(">=", 1)
  val TLe = TOp("<=", 1)
  val TGt = TOp(">", 1)
  val TLt = TOp("<", 1)
  val TAdd = TOp("+", 2)
  val TSub = TOp("-", 2)
  val TCat = TOp("&", 2)
  val TMul = TOp("*", 3)
  val TDiv = TOp("/", 3)
  val TPow = TOp("^", 4, leftAssociative = false)
  val TPos = TOp("+u", 5, leftAssociative = false)
  val TNeg = TOp("-u", 5, leftAssociative = false)

  val cellRefRegex: Regex = """^\$?([a-zA-Z]+)\$?([0-9]+)""".r
  val funcRegex: Regex = """^([a-zA-Z][a-zA-Z0-9_]*)""".r
}


class Lexer(data: String) extends Iterator[(Int, Lexer.Token)] {
  // An optical illusion: you think you're reading scala, but actually it's c

  private var pos: Int = 0
  private var tok: Option[Lexer.Token] = None

  private def error(msg: String) = throw new CellException(msg, pos)

  private def atEof(): Boolean = pos >= data.length
  private def assertNoEof(): Unit = if (atEof()) error("unexpected end of line")

  private def currentChar: Char = { assertNoEof(); data.charAt(pos) }
  private def step(): Unit = { assertNoEof(); pos = pos + 1 }

  private def forceUnary(): Boolean = tok.forall(t => {
    t == Lexer.TOpenPar || t.isInstanceOf[Lexer.TFunc] || t.isInstanceOf[Lexer.TOp]
  })
  private def checkForceNoUnary(op: String): Unit = if (forceUnary()) error(s"$op is not allowed here")
  private def checkForceUnary(op: String): Unit = if (!forceUnary()) error(s"$op is not allowed here")

  override def hasNext: Boolean = !atEof()

  override def next(): (Int, Lexer.Token) = {
    while (!atEof() && currentChar.isSpaceChar) {
      step()
    }

    if (atEof()) {
      return (pos, Lexer.EOF)
    }

    val startPos = pos

    tok = Option((currentChar: @switch) match {
      case ';' => step(); checkForceNoUnary(";"); Lexer.TSemicolon
      case '(' => step(); checkForceUnary("("); Lexer.TOpenPar
      case ')' => step(); Lexer.TClosePar
      case '<' => step(); currentChar match {
        case '=' => step(); checkForceNoUnary("<="); Lexer.TLe
        case '>' => step(); checkForceNoUnary("<>"); Lexer.TNeq
        case _ => checkForceNoUnary("<"); Lexer.TLt
      }
      case '>' => step(); currentChar match {
        case '=' => step(); checkForceNoUnary(">="); Lexer.TGe
        case _ => checkForceNoUnary(">"); Lexer.TGt
      }
      case '=' => step(); checkForceNoUnary("="); Lexer.TEq
      case '+' => step(); if (forceUnary()) Lexer.TPos else Lexer.TAdd
      case '-' => step(); if (forceUnary()) Lexer.TNeg else Lexer.TSub
      case '&' => step(); checkForceNoUnary("&"); Lexer.TCat
      case '*' => step(); checkForceNoUnary("*"); Lexer.TMul
      case '/' => step(); checkForceNoUnary("/"); Lexer.TDiv
      case '^' => step(); checkForceNoUnary("^"); Lexer.TPow
      case '"' | '\'' => eatString()
      case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '.' => eatNumber()
      case c => if (c.isLetterOrDigit || c == '$') eatLiteral() else error("syntax error")
    })

    (startPos, tok.get)
  }

  private def eatString(): Lexer.Token = {
    if (!forceUnary()) {
      error("values should be separated by an operator")
    }

    val delim = currentChar
    val sb = new StringBuilder
    step()
    while (currentChar != delim) {
      if (currentChar == '\\') {
        step()
        currentChar match {
          case 'b' => sb += '\b'
          case 'f' => sb += '\f'
          case 'n' => sb += '\n'
          case 'r' => sb += '\r'
          case 't' => sb += '\t'
          case x   => sb += x
        }
      } else {
        sb += currentChar
      }
      step()
    }

    if (currentChar == delim) step() else error(s"expected $delim, got $currentChar")

    Lexer.TStrLit(sb.toString)
  }

  private def eatLiteral(): Lexer.Token = {
    if (!forceUnary()) {
      error("values should be separated by an operator")
    }

    val sb = new StringBuilder
    while (!atEof() && (currentChar.isLetterOrDigit || currentChar == '$')) {
      sb += currentChar
      step()
    }

    sb.toString.toLowerCase match {
      case Lexer.cellRefRegex(col, row) => Lexer.TCellRef(parseCellRefCol(col) - 1, row.toInt - 1)
      case Lexer.funcRegex(func) => Lexer.TFunc(func)
      case x => error(s"cannot parse a literal $x")
    }
  }

  private def parseCellRefCol(string: String): Int = string.toLowerCase.map(_ - 'a' + 1).reduceLeft(_ * 26 + _)

  private def eatNumber(): Lexer.Token = {
    if (!forceUnary()) {
      error("values should be separated by an operator")
    }

    val sb = new StringBuilder

    var percent = false

    object State extends Enumeration {
      val finish = 0
      val before_decimal_point = 1
      val after_decimal_point = 2
      val after_exponent = 3
      val after_exponent_sign = 4
    }

    var state = State.before_decimal_point

    while (!atEof() && state != State.finish) {
      state match {
        case State.before_decimal_point => (currentChar: @switch) match {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          case 'e' | 'E' => state = State.after_exponent
          case '.' => state = State.after_decimal_point
          case '%' => percent = true; state = State.finish; step()
          case _ => state = State.finish
        }
        case State.after_decimal_point => (currentChar: @switch) match {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          case 'e' | 'E' => state = State.after_exponent
          case '%' => percent = true; state = State.finish; step()
          case _ => state = State.finish
        }
        case State.after_exponent => (currentChar: @switch) match {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' => state = State.after_exponent_sign
          case '+' | '-' => state = State.after_exponent_sign
          case '%' => percent = true; state = State.finish; step()
          case _ => state = State.finish
        }
        case State.after_exponent_sign => (currentChar: @switch) match {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          case '%' => percent = true; state = State.finish; step()
          case _ => state = State.finish
        }
        case _ =>
      }

      if (state != State.finish) {
        sb += currentChar
        step()
      }
    }

    Lexer.TNumLit(sb.toDouble * (if (percent) 0.01 else 1))
  }
}
