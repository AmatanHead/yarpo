package spreadsheets

import org.scalajs.jquery.JQuery

class Sheet(w: Int, h: Int, parent: JQuery) {
  // Do you like spaghetti?
  self =>

  private val cells = for (col <- 0 until w) yield { for (row <- 0 until h) yield new Cell(col, row, self) }
  private val view = new SheetView(w, h, parent, self)

  // View call this when user update cell
  def onCellUpdated(col: Int, row: Int, source: String): Unit = {
    getCell(col, row).updateFormula(source)
  }

  // Controller calls this when it reacts on user updates
  def onCellRecalculated(col: Int, row: Int, source: String, value: String, error: String = ""): Unit = {
    view.updateCell(col, row, source, value, error)
  }

  def getCell(col: Int, row: Int): Cell = {
    if (!(0 <= col && col < w) || !(0 <= row && row < h)) {
      throw new CellException(s"invalid cell index $col:$row", 0)
    }
    cells(col)(row)
  }
}
