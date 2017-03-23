package spreadsheets

class CellExecutionContent(controller: Sheet) {
  self =>

  def evaluate(col: Int, row: Int): (Option[Any], Option[String]) = {
    controller.getCell(col, row).evaluate(self)
  }
}
