package spreadsheets

import org.scalajs.jquery.jQuery

import scala.scalajs.js

object SpreadSheets extends js.JSApp {
  val w = 26 * 2
  val h = 20

  def main(): Unit = {
    val view = new Sheet(w, h, jQuery("#main"))
  }
}
