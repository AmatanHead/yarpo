package spreadsheets

import org.scalajs.jquery.{JQuery, jQuery}

class SheetView(w: Int, h: Int, parent: JQuery, controller: Sheet) {
  private val table = jQuery("<table>"); parent.append(table)

  {
    def nToI(columnNumber: Int): String = {
      var dividend = columnNumber + 1
      var columnName = ""

      while (dividend > 0) {
        val modulo = (dividend - 1) % 26
        columnName = ('A' + modulo).toChar + columnName
        dividend = (dividend - modulo) / 26
      }

      columnName
    }

    val tr = jQuery("<tr>"); table.append(tr)
    tr.append(jQuery("<td>"))
    for (col <- 0 to w) tr.append(jQuery("<th>").text(s"${nToI(col)}"))
  }

  private val cells = for (row <- 0 to h) yield {
    val tr = jQuery("<tr>"); table.append(tr)
    tr.append(jQuery("<th>").text(s"${row + 1}"))
    for (col <- 0 to w) yield {
      val td = jQuery("<td>"); tr.append(td)
      new CellView(td, col, row, controller)
    }
  }

  def updateCell(col: Int, row: Int, source: String, value: String, error: String = ""): Unit = {
    cells(row)(col).updateCell(source, value, error)
  }
}
