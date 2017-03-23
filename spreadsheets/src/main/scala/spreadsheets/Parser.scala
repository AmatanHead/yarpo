package spreadsheets

import scala.collection.mutable

object Parser {
  def parse(s: String): AST = rpn_to_ast(str_to_rpn(s))

  private def str_to_rpn(s: String): mutable.Queue[(Int, Lexer.Token)] = {
    val parenthesisTrack = new mutable.ListBuffer[Int]
    def trackVal() = if (parenthesisTrack.nonEmpty) { parenthesisTrack.remove(0); 1 +=: parenthesisTrack }

    val stack = new mutable.ListBuffer[(Int, Lexer.Token)]()
    val queue = new mutable.Queue[(Int, Lexer.Token)]()

    for ((pos, token) <- new Lexer(s)) {
      if (stack.nonEmpty && stack.head._2.isInstanceOf[Lexer.TFunc] && token != Lexer.TOpenPar) {
        throw new CellException(s"function call should be followed by a parenthesis", pos)
      }

      token match {
        case t: Lexer.TStrLit => queue += ((pos, t)); trackVal()
        case t: Lexer.TNumLit => queue += ((pos, t)); trackVal()
        case t: Lexer.TCellRef => queue += ((pos, t)); trackVal()
        case t: Lexer.TFunc => (pos, t) +=: stack
        case t: Lexer.TOp =>
          while (
            stack.nonEmpty &&
              stack.head._2.isInstanceOf[Lexer.TOp] && (
              stack.head._2.asInstanceOf[Lexer.TOp].precedence > t.precedence ||
                t.leftAssociative && stack.head._2.asInstanceOf[Lexer.TOp].precedence == t.precedence
              )
          ) {
            queue += stack.remove(0)
          }
          (pos, t) +=: stack
        case Lexer.TOpenPar =>
          if (stack.nonEmpty && stack.head._2.isInstanceOf[Lexer.TFunc]) {
            2 +=: parenthesisTrack
          } else {
            0 +=: parenthesisTrack
          }
          (pos, Lexer.TOpenPar) +=: stack
        case Lexer.TClosePar =>
          if (parenthesisTrack.isEmpty) {
            throw new CellException(s"no matching parenthesis", pos)
          } else if (parenthesisTrack.head == 0) {
            throw new CellException(s"parenthesized expression contain no values", pos)
          } else if (parenthesisTrack.head == 2) {
            queue += ((pos, Lexer.TEmptyTuple))
          }

          while (
            stack.nonEmpty &&
              stack.head._2 != Lexer.TOpenPar
          ) {
            queue += stack.remove(0)
          }

          if (stack.isEmpty) throw new CellException(s"no matching parenthesis", pos)

          assert(stack.remove(0)._2 == Lexer.TOpenPar)

          if (stack.nonEmpty && stack.head._2.isInstanceOf[Lexer.TFunc]) {
            queue += stack.remove(0)
          }

          parenthesisTrack.remove(0)
          trackVal()

        case t => throw new CellException(s"unknown token $t", pos)
      }
    }

    while (stack.nonEmpty) {
      if (stack.head._2 == Lexer.TOpenPar || stack.head._2 == Lexer.TClosePar) {
        throw new CellException(s"misplaced parenthesis", stack.head._1)
      }
      queue += stack.remove(0)
    }

    println(s"RPN: $queue")

    queue
  }

  private def rpn_to_ast(queue: mutable.Queue[(Int, Lexer.Token)]): AST = {
    val stack = new mutable.ListBuffer[AST]()

    def get1() = if (stack.isEmpty) throw new CellException("syntax error", 0) else stack.remove(0)
    def get2() = if (stack.length < 2) throw new CellException("syntax error", 0) else (stack.remove(0), stack.remove(0))

    for ((pos, token) <-queue) {
      (token match {
        case t: Lexer.TStrLit => NStr(t.str, pos)
        case t: Lexer.TNumLit => NNum(t.num, pos)
        case t: Lexer.TCellRef => NCellRef(t.col, t.row, pos)
        case t: Lexer.TFunc =>
          val args = get1() match {
            case a: NTuple => a.args
            case a => a :: Nil
          }
          NFunc(t.func, args, pos)
        case Lexer.TPos => NUnOp(Lexer.TPos, get1(), pos)
        case Lexer.TNeg => NUnOp(Lexer.TNeg, get1(), pos)
        case Lexer.TEmptyTuple => NTuple(Nil, pos)
        case Lexer.TSemicolon => get2() match {
          case (NTuple(args1, _), NTuple(args2, _)) => NTuple(args1 ::: args2, pos)
          case (NTuple(args1, _), x) => NTuple(args1 ::: x :: Nil, pos)
          case (x, NTuple(args2, _)) => NTuple(x :: args2, pos)
          case (x, y) => NTuple(x :: y :: Nil, pos)
        }
        case t: Lexer.TOp =>
          val (b, a) = get2()
          NBinOp(t.op, a, b, pos)
        case t => throw new CellException(s"unknown token $t", pos)
      }) +=: stack
    }

     if (stack.length != 1) {
       throw new CellException("syntax error", 0)
     }

    println(s"AST: ${stack.head}")

    stack.head
  }
}
