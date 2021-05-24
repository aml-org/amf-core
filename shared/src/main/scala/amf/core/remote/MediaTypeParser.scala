package amf.core.remote

import scala.util.matching.Regex

class MediaTypeParser(mediaType: String) {

  // val MediaTypeRegexp: Regex = "(\\w+)\/([-.\\w])+(?:\\+[-.\\w]+)?".r

  private lazy val (base, v, syntax) = mediaType.split("/").toList match {
    case Nil         => ("application", "unknown", None)
    case base :: Nil => (base, "unknown", None)
    case base :: _ =>
      val exp = mediaType.stripPrefix(base + "/")
      exp.split("\\+").toList match {
        case Nil          => (base, "unknown", None)
        case head :: Nil  => (base, head, None)
        case head :: tail => (base, head, Some(exp.stripPrefix(head + "+")))
      }
  }

  def getSyntaxExp: Option[String] = syntax.map(s => base + "/" + s)

  def getVendorExp: String     = mediaType
  def getPureVendorExp: String = base + "/" + v

}
