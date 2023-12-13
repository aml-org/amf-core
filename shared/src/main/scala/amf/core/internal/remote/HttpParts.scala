package amf.core.internal.remote

object HttpParts {
  def unapply(uri: String): Option[(String, String, String)] = uri match {
    case url if url.startsWith(HTTP_PROTOCOL) || url.startsWith(HTTPS_PROTOCOL) =>
      val protocol        = url.substring(0, url.indexOf("://") + 3)
      val rightOfProtocol = url.stripPrefix(protocol)
      val host =
        if (rightOfProtocol.contains("/")) rightOfProtocol.substring(0, rightOfProtocol.indexOf("/"))
        else rightOfProtocol
      val path = rightOfProtocol.replace(host, "")
      Some(protocol, host, path)
    case _ => None
  }

  val HTTP_PROTOCOL  = "http://"
  val HTTPS_PROTOCOL = "https://"
}
