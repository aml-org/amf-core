package amf.core.internal.remote.server

import amf.core.client.common.remote.Content
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.scala.resource.{FileResourceLoader => InternalFileResourceLoader}
import amf.core.internal.unsafe.PlatformSecrets

import java.io.IOException
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("JsServerFileResourceLoader")
@JSExportAll
case class JsServerFileResourceLoader() extends ResourceLoader with PlatformSecrets {

  private val internal =
    InternalFileResourceLoader(platform.fs, e => e.isInstanceOf[IOException])(ExecutionContext.global)

  /** Fetch specified resource and return associated content. Resource should have been previously accepted.
    * If the resource doesn't exists, it returns a failed future caused by a ResourceNotFound exception. */
  override def fetch(resource: String): js.Promise[Content] = {
    internal.fetch(resource).toJSPromise
  }

  /** Checks if the resource loader accepts the specified resource. */
  override def accepts(resource: String): Boolean = internal.accepts(resource)
}
