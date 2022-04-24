package amf.core.client.scala.resource

import amf.core.client.common.remote.Content
import amf.core.client.scala.lexer.{CharSequenceStream, CharStream}
import amf.core.client.scala.resource.FileResourceLoader.defaultCatch
import amf.core.internal.remote.File.FILE_PROTOCOL
import amf.core.internal.remote.FileMediaType.{extension, mimeFromExtension}
import amf.core.internal.remote.{File, FileNotFound}
import amf.core.internal.utils.AmfStrings
import org.mulesoft.common.io.FileSystem

import java.io.{FileNotFoundException, IOException}
import scala.concurrent.{ExecutionContext, Future}

object FileResourceLoader {
  protected val defaultCatch: Throwable => Boolean = {
    case _: IOException => true
  }
}

case class FileResourceLoader(private val fs: FileSystem,
                              private val shouldCatch: Throwable => Boolean = defaultCatch)(
    private implicit val executionContext: ExecutionContext)
    extends ResourceLoader {

  override def fetch(resource: String): Future[Content] = fetchFile(resource.stripPrefix(FILE_PROTOCOL))

  override def accepts(resource: String): Boolean = resource match {
    case File(_) => true
    case _       => false
  }

  private def fetchFile(resource: String): Future[Content] = {
    buildContent(resource)
      .recoverWith {
        case e: Throwable if shouldCatch(e) => buildContent(resource.urlDecoded, resource)
      }
      .recover {
        case e: Throwable if shouldCatch(e) => throw FileNotFound(e)
      }
  }

  private def buildContent(resource: String): Future[Content] = buildContent(resource, resource)

  private def buildContent(resource: String, originalResource: String): Future[Content] = {
    readFileContents(resource).map { content =>
      Content(new CharSequenceStream(originalResource, content),
              ensureFileURIScheme(originalResource),
              extension(originalResource).flatMap(mimeFromExtension))
    }
  }

  private def readFileContents(resource: String): Future[String] = fs.asyncFile(resource).read().map(_.toString)

  private def ensureFileURIScheme(str: String): String = if (str.startsWith("file:")) str else s"file://$str"
}
