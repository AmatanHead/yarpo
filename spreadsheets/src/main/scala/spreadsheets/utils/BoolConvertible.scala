package spreadsheets.utils

object BoolConvertible {
  def unapply(arg: Any): Option[(Boolean)] = arg match {
    case b: Boolean => Option(b)
    case d: Double => Option(!(d.abs <= Double.MinPositiveValue))  // uncomment to enable num->bool conversion
    case "true" => Option(true)
    case "false" => Option(false)
    // case s: String => Option(s.length > 0)  // uncomment to enable string->bool conversion
    case _ => None
  }
}
