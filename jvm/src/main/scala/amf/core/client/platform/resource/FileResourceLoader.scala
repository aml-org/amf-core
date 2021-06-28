package amf.core.client.platform.resource

import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.common.remote.Content
import amf.core.client.scala.lexer.FileStream
import amf.core.internal.remote.FileMediaType._
import amf.core.internal.remote.FutureConverter._
import amf.core.internal.remote.{FileNotFound, JvmPlatform}
import amf.core.internal.utils.AmfStrings

import scala.concurrent.{ExecutionContext, Future}

case class FileResourceLoader(executionContext: ExecutionContext)
    extends BaseFileResourceLoader
    with LoaderWithExecutionContext {

  implicit val exec: ExecutionContext = executionContext

  def this() = this(JvmPlatform.instance().defaultExecutionEnvironment.executionContext)
  def this(executionEnvironment: BaseExecutionEnvironment) = this(executionEnvironment.executionContext)

  override def withExecutionContext(newEc: ExecutionContext): ResourceLoader = FileResourceLoader(newEc)

  def fetchFile(resource: String): CompletableFuture[Content] = {
    Future {
      try {
        Content(new FileStream(resource),
                ensureFileAuthority(resource),
                extension(resource).flatMap(mimeFromExtension))
      } catch {
        case e: FileNotFoundException =>
          // exception for local file system where we accept spaces [] and other chars in files names
          val decoded = resource.urlDecoded
          try {
            Content(new FileStream(decoded),
                    ensureFileAuthority(resource),
                    extension(resource).flatMap(mimeFromExtension))
          } catch {
            case e: FileNotFoundException => throw FileNotFound(e)
          }
      }
    }.asJava
  }

  def ensureFileAuthority(str: String): String = if (str.startsWith("file:")) str else s"file://$str"
}
