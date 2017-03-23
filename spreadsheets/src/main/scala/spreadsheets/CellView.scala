package spreadsheets

import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}

class CellView(parent: JQuery, col: Int, row: Int, controller: Sheet) {
  private val div = jQuery("<div>"); parent.append(div)
  private val view = jQuery("<pre>"); div.append(view)
  private var input: Option[JQuery] = None

  private var source: String = ""
  private var value: String = ""

  div.addClass("cell")
  view.addClass("view")

  private def onClick(): Unit = {
    if (input.isEmpty) {
      view.addClass("edit")

      input = Option(jQuery("<input>"))
      input.get.value(source)
      div.append(input.get)
      input.get.focus()
      input.get.focusout(onOk)
      input.get.keyup((e: JQueryEventObject) => { e.keyCode.getOrElse(-1) match { case 13 => onOk(e) case 27 => onReject(e) case _ => } })
    }
  }

  private def onOk(e: JQueryEventObject): Unit = {
    e.preventDefault()
    if (input.nonEmpty) {
      div.removeClass("edit")
      source = input.get.value().toString
      controller.onCellUpdated(col, row, source)
      input.get.remove()
      input = None
    }
  }

  private def onReject(e: JQueryEventObject): Unit = {
    e.preventDefault()
    if (input.nonEmpty) {
      div.removeClass("edit")
      input.get.remove()
      input = None
    }
  }

  parent.click(() => onClick())

  def updateCell(_source: String, _value: String, _error: String = ""): Unit = {
    source = _source
    value = _value
    view.text(value)
    input.map(_.value(source))

    if (_error.length > 0) {
      view.addClass("error")
      view.attr("title", _error)
    } else {
      view.removeClass("error")
      view.attr("title", "")
    }

  }
}
