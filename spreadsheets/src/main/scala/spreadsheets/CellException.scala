package spreadsheets

class CellException(msg: String, pos: Int) extends RuntimeException(msg) {
  def formatException(source: String): String = s"$msg at $pos" // ":\n$source\n${" " * pos}^"
}
