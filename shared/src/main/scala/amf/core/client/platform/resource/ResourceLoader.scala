package amf.core.client.platform.resource

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.common.remote.Content

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

trait ResourceLoader {

  /** Fetch specified resource and return associated content. Resource should have been previously accepted. */
  /** If the resource not exists, you should return a future failed with an ResourceNotFound exception. */
  def fetch(resource: String, ec: ExecutionContext): ClientFuture[Content]

  /** Accepts specified resource. */
  def accepts(resource: String): Boolean = true
}

@JSExportAll
@JSExportTopLevel("ResourceLoaderFactory")
object ResourceLoaderFactory {
  def create(loader: ClientResourceLoader) = new ResourceLoader {
    override def accepts(resource: String): Boolean                                   = loader.accepts(resource)
    override def fetch(resource: String, ec: ExecutionContext): ClientFuture[Content] = loader.fetch(resource)
  }
}
