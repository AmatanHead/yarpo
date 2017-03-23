package spreadsheets

import scala.collection.mutable


class Cell(col: Int, row: Int, controller: Sheet) {
  private var source: String = ""
  private var formula: AST = NStr("", 0)

  private var value: Option[Any] = None
  private var error: Option[String] = None
  private var dependsOn: Set[(Int, Int)] = Set()
  private var dependsOnMe: mutable.Set[(Int, Int)] = mutable.Set()

  def addDependsOnMe(col: Int, row: Int): Unit = dependsOnMe += ((col, row))
  def removeDependsOnMe(col: Int, row: Int): Unit = dependsOnMe -= ((col, row))

  def updateFormula(_source: String): Unit = {
    try {
      source = _source
      if (source startsWith "=") {
        formula = Parser.parse(source.stripPrefix("="))
      } else {
        formula = NStr(source, 0)
      }

      for ((_col, _row) <- dependsOn) {
        controller.getCell(_col, _row).removeDependsOnMe(col, row)
      }
      dependsOn = formula.getDependencies()
      for ((_col, _row) <- dependsOn) {
        controller.getCell(_col, _row).addDependsOnMe(col, row)
      }

      invalidate()

      evaluateDependants(new CellExecutionContent(controller))
//      evaluate(new CellExecutionContent(controller))
    } catch {
      case e: CellException =>
        setError("error in dependant cell")
        value = None
        error = Option(e.formatException(source))
        controller.onCellRecalculated(col, row, source, "#ERROR", error.get)
    }
  }

  private def invalidate(visited: mutable.Set[(Int, Int)] = mutable.Set()): Unit = {
    if (visited contains (col, row)) {
      throw new CellException("circular reference", 0)
    } else {
      visited += ((col, row))
    }

    error = None
    value = None

    controller.onCellRecalculated(col, row, source, "")

    for ((_col, _row) <- dependsOnMe) {
      controller.getCell(_col, _row).invalidate(visited)
    }
  }

  private def setError(_error: String, visited: mutable.Set[(Int, Int)] = mutable.Set()): Unit = {
    if (visited contains (col, row)) {
      return
    } else {
      visited += ((col, row))
    }

    error = Option(_error)
    value = None

    controller.onCellRecalculated(col, row, source, "#ERROR", _error)

    for ((_col, _row) <- dependsOnMe) {
      controller.getCell(_col, _row).setError(_error, visited)
    }
  }

  private def evaluateDependants(c: CellExecutionContent, visited: mutable.Set[(Int, Int)] = mutable.Set()): Unit = {
    println(visited, dependsOnMe)
    if (visited contains (col, row)) {
      return
    } else {
      visited += ((col, row))
    }

    evaluate(c)

    for ((_col, _row) <- dependsOnMe) {
      controller.getCell(_col, _row).evaluateDependants(c, visited)
    }
  }

  def evaluate(c: CellExecutionContent): (Option[Any], Option[String]) = {
    if (value.nonEmpty || error.nonEmpty) {
      return (value, error)
    }

    try {
      value = Option(formula.eval(c))
      controller.onCellRecalculated(col, row, source, value.get.toString)
    } catch {
      case e: CellException =>
        value = None
        error = Option(e.formatException(source))
        controller.onCellRecalculated(col, row, source, "#ERROR", error.get)
    }
    (value, error)
  }

//  def invalidate(c: CellExecutionContent): Unit = {
//    if (value.nonEmpty || error.nonEmpty) {
//      value = None
//      error = None
//      for ((x, y) <- dependsOn) {
//        c.invalidate(x, y)
//      }
//    }
//  }
//
//  def evaluate(c: CellExecutionContent): Option[Any] = {
//    if (value.isEmpty && error.isEmpty) {
//      dependsOn = formula.getDependencies()
//
//      try {
//        value = Option(formula.eval(c))
//      } catch {
//        case e: CellException => error = Option(e.formatException(source))
//      }
//    }
//
//    value
//  }
//
//  def setNewFormula(c: CellExecutionContent, _source: String, _formula: AST): Unit = {
//    invalidate(c)
//    source = _source
//    formula = _formula
//    evaluate(c)
//  }
}
