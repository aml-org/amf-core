package amf.core.internal.remote.server

import amf.core.client.common.remote.Content
import amf.core.client.platform.resource.BaseFileResourceLoader
import amf.core.client.scala.lexer.CharSequenceStream
import amf.core.internal.remote.FileMediaType._
import amf.core.internal.remote.FileNotFound
import amf.core.internal.utils.AmfStrings
import org.mulesoft.common.io.Fs

import java.io.IOException
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("JsServerFileResourceLoader")
@JSExportAll
case class JsServerFileResourceLoader() extends BaseFileResourceLoader {

  implicit def executionContext: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.queue
  override def fetchFile(resource: String): js.Promise[Content] =
    Fs.asyncFile(resource)
      .read()
      .map(content =>
        Content(
          new CharSequenceStream(resource, content),
          ensureFileAuthority(resource),
          extension(resource).flatMap(mimeFromExtension)
        )
      )
      .recoverWith {
        case _: IOException => // exception for local file system where we accept resources including spaces
          Fs.asyncFile(resource.urlDecoded)
            .read()
            .map(content =>
              Content(
                new CharSequenceStream(resource, content),
                ensureFileAuthority(resource),
                extension(resource).flatMap(mimeFromExtension)
              )
            )
            .recover { case io: IOException =>
              throw FileNotFound(io)
            }
      }
      .toJSPromise

  def ensureFileAuthority(str: String): String = if (str.startsWith("file:")) str else s"file://$str"
}
