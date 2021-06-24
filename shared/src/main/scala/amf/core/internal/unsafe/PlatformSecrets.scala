package amf.core.internal.unsafe

import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.execution.ExecutionEnvironment
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.rdf.RdfModel
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.remote.{Platform, UnsupportedFileSystem}
import amf.core.internal.validation.core.{
  SHACLValidator,
  ShaclValidationOptions,
  ValidationReport,
  ValidationSpecification
}
import org.mulesoft.common.io.FileSystem

import scala.concurrent.{ExecutionContext, Future}

trait PlatformSecrets {
  val platform: Platform = PlatformBuilder()
}

// TODO: Removed from core @modularization
/*
class TrunkDialectsRegistry(platform: Platform) extends PlatformDialectRegistry(platform) {
  add(VocabularyLanguageDefinition)
  add(DialectLanguageDefinition)

  override def registerDialect(uri: String) = throw new Exception("Not supported in trunk platform")

  override def registerDialect(uri: String, dialect: String) = throw new Exception("Not supported in trunk platform")
}
 */

class TrunkValidator extends SHACLValidator {
  override def validate(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext) =
    throw new Exception("Error, validation is not supported")

  override def report(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext) =
    throw new Exception("Error, validation is not supported")

  /**
    * Registers a library in the validator
    *
    * @param url
    * @param code
    * @return
    */
  override def registerLibrary(url: String, code: String): Unit =
    throw new Exception("Error, validation is not supported")

  override def validate(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[String] =
    throw new Exception("Error, validation is not supported")

  override def report(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ShaclValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] =
    throw new Exception("Error, validation is not supported")

  override def emptyRdfModel(): RdfModel = throw new Exception("Error, validation is not supported")

  override def shapes(shapes: Seq[ValidationSpecification], functionsUrl: String): RdfModel =
    throw new Exception("Error, validation is not supported")

  override def supportsJSFunctions: Boolean = false
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

  override val defaultExecutionEnvironment: BaseExecutionEnvironment = new BaseExecutionEnvironment(
      ExecutionEnvironment()) {}
}
