package amf.core.client.scala.parse.document

import amf.core.client.common.position.Range
import amf.core.client.scala.model.document._
import amf.core.internal.remote.File.FILE_PROTOCOL
import amf.core.internal.remote.HttpParts.{HTTPS_PROTOCOL, HTTP_PROTOCOL}
import amf.core.internal.utils.AmfStrings
import org.mulesoft.lexer.SourceLocation

case class ReferenceResolutionResult(exception: Option[Throwable], unit: Option[BaseUnit])

trait RefContainer {
  val linkType: ReferenceKind
  val uriFragment: Option[String]
  def reduceToLocation(): Range
}

class ASTRefContainer(override val linkType: ReferenceKind, val pos: SourceLocation, override val uriFragment: Option[String]) extends RefContainer {
  override def reduceToLocation(): Range = Range((pos.lineFrom,pos.columnFrom),(pos.lineTo, pos.columnTo))
}

object ASTRefContainer {
  def apply(linkType: ReferenceKind, pos: SourceLocation, uriFragment: Option[String]) = new ASTRefContainer(linkType, pos, uriFragment)
}

class CompilerReferenceCollector() {
  protected val collector = DefaultReferenceCollector[Reference]()

  def +=(key: String, kind: ReferenceKind, pos: SourceLocation): Unit = {
    val (url, fragment) = ReferenceFragmentPartition(key)
    collector.get(url) match {
      case Some(reference: Reference) =>
        val refContainer =  ASTRefContainer(kind, pos, fragment)
        collector += (url, reference + refContainer)
      case None                       =>
        collector += (url, Reference(url, kind, pos, fragment))
    }
  }

  def toReferences: Seq[Reference] = collector.references()
}

object CompilerReferenceCollector {
  def apply() = new CompilerReferenceCollector()
}

object EmptyReferenceCollector extends CompilerReferenceCollector {}

/**
  * Splits references between their base url and local path
  * E.g. https://some.path.json#/local/path -> ("https://some.path.json", "local/path")
  */
object ReferenceFragmentPartition {
  // Is it always a URL? If we can have local references then it is not a URL
  def apply(url: String): (String, Option[String]) = {
    if (isExternalReference(url)) {
      url.split("#") match { // how can i know if the # its part of the uri or not? uri not valid???
        case Array(basePath) if basePath.endsWith("#") => (basePath.substring(0, basePath.length - 2), None)
        case Array(basePath)                           => (basePath, None)
        case Array(basePath, localPath)                => (basePath, Some(localPath))
        case other                                     =>
          //  -1 of the length diff and -1 for # char
          val str = url.substring(0, url.length - 1 - other.last.length)
          (str, Some(other.last))
      }
    }
    else (url, None)
  }

  /**
    * Checks if reference corresponds to a remote resource
    * @param referenceLocator like an URL but not uniform since we can have local references
    * @return
    */
  private def isExternalReference(referenceLocator: String) = {
    (referenceLocator.normalizeUrl.startsWith(FILE_PROTOCOL) || referenceLocator.startsWith(HTTPS_PROTOCOL) || referenceLocator
      .startsWith(HTTP_PROTOCOL)) && !referenceLocator
      .startsWith("#")
  }
}
