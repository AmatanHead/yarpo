package spreadsheets

import spreadsheets.utils.{BoolConvertible, DoubleConvertible}

sealed abstract class AST(pos: Int) {
  def eval(c: CellExecutionContent): Any
  //noinspection AccessorLikeMethodIsEmptyParen
  def getDependencies(): Set[(Int, Int)] = Set()
  protected def error(msg: String) = throw new CellException(msg, pos)
  val position: Int = pos
}

sealed case class NCellRef(col: Int, row: Int, pos: Int) extends AST(pos) {
  def eval(c: CellExecutionContent): Any = {
    val (res, err) = c.evaluate(col, row)
    if (res.isEmpty) {
      error("error in dependant cell: " + err.getOrElse("unknown error"))
    } else {
      res.get
    }
  }

  override def getDependencies(): Set[(Int, Int)] = Set((col, row))
}

sealed case class NNum(n: Double, pos: Int) extends AST(pos) {
  def eval(c: CellExecutionContent): Double = n
}

sealed case class NStr(s: String, pos: Int) extends AST(pos) {
  def eval(c: CellExecutionContent): String = s
}

sealed case class NTuple(args: List[AST], pos: Int) extends AST(pos) {
  def eval(c: CellExecutionContent): Unit = error("tuples cannot be evaluated; did you misplaced semicolon?")
  override def getDependencies(): Set[(Int, Int)] = args.map(_.getDependencies()).reduce(_ | _)
}

sealed case class NBool(b: Boolean, pos: Int) extends AST(pos) {
  def eval(c: CellExecutionContent): Boolean = b
}

sealed case class NFunc(f: String, args: List[AST], pos: Int) extends AST(pos) {
  def error(x: Any, t: String): Nothing = error(
    s"cannot apply function $f to $x: argument cannot be converted to $t"
  )

  private val fx: CellExecutionContent => Any = (f.toLowerCase, args) match {
    case ("true", Nil) => (c: CellExecutionContent) => true
    case ("false", Nil) => (c: CellExecutionContent) => false
    case ("sin", x :: Nil) => (c: CellExecutionContent) => x.eval(c) match {
      case DoubleConvertible(res) => Math.sin(res)
      case res => error(res, "double")
    }
    case ("abs", x :: Nil) => (c: CellExecutionContent) => x.eval(c) match {
      case DoubleConvertible(res) => Math.abs(res)
      case res => error(res, "double")
    }
    case ("len", x :: Nil) => (c: CellExecutionContent) => x.eval(c) match {
      case res: String => res.length
      case res => error(res, "string")
    }
    case ("if", cond :: a :: b :: Nil) => (c: CellExecutionContent) => cond.eval(c) match {
      case BoolConvertible(res) => if (res) a.eval(c) else b.eval(c)
      case res => error(res, "boolean")
    }
    case ("and", l) => (c: CellExecutionContent) => l.forall(res => {
      res.eval(c) match {
        case BoolConvertible(e) => e
        case e => error(e, "boolean") }
    })
    case ("or", l) => (c: CellExecutionContent) => !l.forall(res => {
      res.eval(c) match {
        case BoolConvertible(e) => !e
        case e => error(e, "boolean") }
    })
    case ("not", x :: Nil) => (c: CellExecutionContent) => x.eval(c) match {
      case BoolConvertible(res) => !res
      case res => error(res, "boolean")
    }
    case _ => error(s"cannot find function $f with ${args.length} argument(s)")
  }

  def eval(c: CellExecutionContent): Any = fx(c)

  override def getDependencies(): Set[(Int, Int)] = args.map(_.getDependencies()).reduce(_ | _)
}

sealed case class NBinOp(op: String, left: AST, right: AST, pos: Int) extends AST(pos) {
  def error(x: Any, t: String): Nothing = error(
    s"cannot apply operator $op to $x: argument cannot be converted to $t"
  )

  private val fx = op match {
    case Lexer.TEq.op /* "=" */ => (c: CellExecutionContent) => left.eval(c) == right.eval(c)
    case Lexer.TNeq.op /* "<>" */ => (c: CellExecutionContent) => left.eval(c) != right.eval(c)
    case Lexer.TGe.op /* ">=" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a >= b
      case x => error(x, "double")
    }
    case Lexer.TLe.op /* "<=" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a <= b
      case x => error(x, "double")
    }
    case Lexer.TGt.op /* ">" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a > b
      case x => error(x, "double")
    }
    case Lexer.TLt.op /* "<" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a < b
      case x => error(x, "double")
    }
    case Lexer.TAdd.op /* "+" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a + b
      case x => error(x, "double")
    }
    case Lexer.TSub.op /* "-" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a - b
      case x => error(x, "double")
    }
    case Lexer.TCat.op /* "&" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (a: String, b: String) => a + b
      case x => error(x, "string")
    }
    case Lexer.TMul.op /* "*" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a * b
      case (a: String, DoubleConvertible(b)) if b.isWhole => a * b.toInt
      case x => error(x, "string, int")
    }
    case Lexer.TDiv.op /* "/" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => a / b
      case x => error(x, "double")
    }
    case Lexer.TPow.op /* "^" */ => (c: CellExecutionContent) => (left.eval(c), right.eval(c)) match {
      case (DoubleConvertible(a), DoubleConvertible(b)) => Math.pow(a, b)
      case x => error(x, "double")
    }
    case _ => error(s"unknown binary operator $op")
  }

  def eval(c: CellExecutionContent): Any = fx(c)

  override def getDependencies(): Set[(Int, Int)] = left.getDependencies() | right.getDependencies()
}

sealed case class NUnOp(op: Lexer.TOp, right: AST, pos: Int) extends AST(pos) {
  private val fx = op match {
    case Lexer.TNeg /* "-u" */ => (c: CellExecutionContent) => right.eval(c) match {
      case DoubleConvertible(x) => -x
      case x => error(s"cannot negate non-number $x")
    }
    case Lexer.TPos /* "+u" */ => (c: CellExecutionContent) => right.eval(c) match {
      case DoubleConvertible(x) => -x
      case x => error(s"cannot apply operation to non-number $x")
    }
    case _ => error(s"unknown unary operator $op")
  }

  def eval(c: CellExecutionContent): Any = fx(c)

  override def getDependencies(): Set[(Int, Int)] = right.getDependencies()
}
