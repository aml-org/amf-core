package amf.core.client.platform.resource

import amf.core.client.common.remote.Content
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.scala.resource.{FileResourceLoader => InternalFileResourceLoader}
import amf.core.internal.remote.FileNotFound
import amf.core.internal.remote.FutureConverter._
import amf.core.internal.unsafe.PlatformSecrets

import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContext

case class FileResourceLoader(executionContext: ExecutionContext)
    extends ResourceLoader
    with PlatformSecrets
    with LoaderWithExecutionContext {

  implicit val exec: ExecutionContext = executionContext
  private val internal                = InternalFileResourceLoader(platform.fs, e => e.isInstanceOf[FileNotFoundException])

  def this() = this(scala.concurrent.ExecutionContext.Implicits.global)
  def this(executionEnvironment: BaseExecutionEnvironment) = this(executionEnvironment.executionContext)

  override def fetch(resource: String): CompletableFuture[Content] = internal.fetch(resource).asJava

  override def accepts(resource: String): Boolean = internal.accepts(resource)

  override def withExecutionContext(newEc: ExecutionContext): ResourceLoader = FileResourceLoader(newEc)
}
