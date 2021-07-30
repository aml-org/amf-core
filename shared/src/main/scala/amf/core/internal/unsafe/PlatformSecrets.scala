package amf.core.internal.unsafe

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.remote.{Platform, UnsupportedFileSystem}
import amf.core.internal.validation.core.{ShaclValidationOptions, ValidationReport, ValidationSpecification}
import org.mulesoft.common.io.FileSystem

import scala.concurrent.{ExecutionContext, Future}

trait PlatformSecrets {
  val platform: Platform = PlatformBuilder()
}

case class TrunkPlatform(content: String,
                         wrappedPlatform: Option[Platform] = None,
                         forcedMediaType: Option[String] = None)
    extends Platform {

  /** Underlying file system for platform. */
  override val fs: FileSystem = UnsupportedFileSystem

  /** Test path resolution. */
  override def resolvePath(path: String): String = path

  override def tmpdir(): String = throw new Exception("Unsupported tmpdir operation")

  override def fetchContent(url: String, configuration: AMFGraphConfiguration)(
      implicit executionContext: ExecutionContext): Future[Content] =
    Future.successful(new Content(content, url, forcedMediaType))

  /** Platform out of the box [ResourceLoader]s */
  override def loaders()(implicit executionContext: ExecutionContext): Seq[ResourceLoader] =
    wrappedPlatform.map(_.loaders()).getOrElse(Seq())

  override def findCharInCharSequence(s: CharSequence)(p: Char => Boolean): Option[Char] =
    wrappedPlatform.flatMap(_.findCharInCharSequence(s)(p))

  /** encodes a complete uri. Not encodes chars like / */
  override def encodeURI(url: String): String = url

  /** decode a complete uri. */
  override def decodeURI(url: String): String = url

  /** encodes a uri component, including chars like / and : */
  override def encodeURIComponent(url: String): String = url

  /** decodes a uri component */
  override def decodeURIComponent(url: String): String = url

  override def normalizeURL(url: String): String = url

  override def normalizePath(url: String): String = url

  /** Return the OS (win, mac, nux). */
  override def operativeSystem(): String = "trunk"
}
