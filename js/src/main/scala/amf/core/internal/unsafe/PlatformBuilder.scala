package amf.core.internal.unsafe

import amf.core.internal.remote.JsPlatform
import amf.core.internal.remote.browser.JsBrowserPlatform
import amf.core.internal.remote.server.JsServerPlatform

import scala.scalajs.js

object PlatformBuilder {

  val platform: JsPlatform = if (isBrowser) new JsBrowserPlatform() else new JsServerPlatform()

  def apply(): JsPlatform = platform

  /** Return true if js is running on browser. */
  private def isBrowser: Boolean = js.typeOf(js.Dynamic.global.window) != "undefined"
}
