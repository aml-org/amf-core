package amf.core.client.common.remote

import amf.core.client.scala.lexer.{CharSequenceStream, CharStream}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

case class Content(stream: CharStream, url: Option[String], mime: Option[String] = None) {

  @JSExportTopLevel("Content")
  def this(stream: String) = this(new CharSequenceStream("", stream), None)

  @JSExportTopLevel("Content")
  def this(stream: String, url: String) = this(new CharSequenceStream(url, stream), Some(url))

  @JSExportTopLevel("Content")
  def this(stream: String, url: String, mime: String) =
    this(new CharSequenceStream(url, stream), Some(url), Some(mime))

  def this(stream: String, url: String, mime: Option[String]) =
    this(new CharSequenceStream(url, stream), Some(url), mime)

  def this(stream: CharStream, url: String) = this(stream, Some(url), None)
  def this(stream: CharStream, url: String, mime: Option[String]) = this(stream, Some(url), mime)

  override def toString: String = stream.toString
}

object Content {
  def apply(stream: CharStream, url: String)                       = new Content(stream, Some(url), None)
  def apply(stream: CharStream, url: String, mime: Option[String]) = new Content(stream, Some(url), mime)
}
