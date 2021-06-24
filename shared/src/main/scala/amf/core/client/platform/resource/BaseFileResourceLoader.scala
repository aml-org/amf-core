package amf.core.client.platform.resource

import amf.core.internal.convert.CoreClientConverters._
import amf.core.client.common.remote.Content
import amf.core.internal.remote.File
import amf.core.internal.remote.File.FILE_PROTOCOL

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait BaseFileResourceLoader extends ResourceLoader {
  override def fetch(resource: String, ec: ExecutionContext): ClientFuture[Content] =
    fetchFile(resource.stripPrefix(FILE_PROTOCOL), ec)

  def fetchFile(resource: String, ec: ExecutionContext): ClientFuture[Content]

  override def accepts(resource: String): Boolean = resource match {
    case File(_) => true
    case _       => false
  }
}
