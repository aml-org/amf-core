package amf.core.internal.remote

import amf.core.client.common.remote.Content
import amf.core.client.scala.lexer.CharStream

/**
  *
  */
case class InternalContent(stream: CharStream, url: String, mime: Option[String] = None)

object InternalContent {

  /**
    * ClientContent may not define a url, for this case a default url is defined.
    */
  def withUrl(content: Content, defaultUrl: String): InternalContent = {
    InternalContent(content.stream, content.url.getOrElse(defaultUrl), content.mime)
  }
}
