package amf.core.internal.remote

import amf.core.internal.remote.platform.PlatformRegex

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.URIUtils

trait JsPlatform extends Platform {

  override def name = "js"

  override def findCharInCharSequence(stream: CharSequence)(p: Char => Boolean): Option[Char] = stream.toString.find(p)

  /** encodes a complete uri. Not encodes chars like / */
  override def encodeURI(url: String): String = URIUtils.encodeURI(url)

  /** encodes a uri component, including chars like / and : */
  override def encodeURIComponent(url: String): String = URIUtils.encodeURIComponent(url)

  /** decode a complete uri. */
  override def decodeURI(url: String): String = URIUtils.decodeURI(url)

  /** decodes a uri component */
  override def decodeURIComponent(url: String): String = URIUtils.decodeURIComponent(url)

  override val globalExecutionContext: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue

  override def regex(regex: String): PlatformRegex = JsNativeRegex(regex)
}

object JsNativeRegex {
  def apply(pattern: String): PlatformRegex = JsNativeRegex(js.RegExp(pattern))
}

case class JsNativeRegex(private val regex: js.RegExp) extends PlatformRegex {
  override def test(value: String): Boolean = regex.test(value)

  override def findFirstIn(value: String): Option[String] = regex.exec(value).head.toOption
}
