package spreadsheets.utils

object DoubleConvertible {
  def unapply(arg: Any): Option[(Double)] = arg match {
    case d: Double => Option(d)
    case d: Boolean => Option(if (d) 1 else 0)
    case s: String =>
      val res = try { Option(s.stripSuffix("%").toDouble) } catch { case _: java.lang.NumberFormatException => None }
      if (s.endsWith("%")) res.map(_ * 0.01) else res
    case _ => None
  }
}
