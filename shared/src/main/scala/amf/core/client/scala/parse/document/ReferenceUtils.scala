package amf.core.client.scala.parse.document

import amf.core.client.common.position.Range
import amf.core.client.scala.model.document._
import amf.core.internal.remote.File.FILE_PROTOCOL
import amf.core.internal.remote.HttpParts.{HTTPS_PROTOCOL, HTTP_PROTOCOL}
import amf.core.internal.utils.AmfStrings
import org.mulesoft.antlrast.ast.ASTElement
import org.yaml.model.YNode.MutRef
import org.yaml.model.{YNode, YScalar}

case class ReferenceResolutionResult(exception: Option[Throwable], unit: Option[BaseUnit])

trait RefContainer {
  val linkType: ReferenceKind
  val uriFragment: Option[String]
  def reduceToLocation(): Range
}

case class AntlrRefContainer(override val linkType: ReferenceKind, node: ASTElement, override val uriFragment: Option[String]) extends RefContainer {
  override def reduceToLocation(): Range = {
    Range((node.start.line, node.start.column),(node.end.line, node.end.column))
  }
}

case class SYamlRefContainer(override val linkType: ReferenceKind, node: YNode, override val uriFragment: Option[String]) extends RefContainer {

  override def reduceToLocation(): Range = {
    node.asOption[YScalar] match {
      case Some(s)  =>
        reduceStringLength(s, uriFragment.map(l => l.length + 1).getOrElse(0), if(s.mark.plain) 0  else 1)
      case _ if node.isInstanceOf[MutRef] =>
        val mutRef = node.asInstanceOf[MutRef]
        Range(mutRef.origValue.range)
      case _ => Range(node.location.inputRange)
    }
  }

  private def reduceStringLength(s:YScalar, fragmentLength: Int, markSize:Int = 0): Range = {
    val inputRange = if(node.location.inputRange.columnTo < fragmentLength && node.location.inputRange.lineFrom< node.location.inputRange.lineTo) {
      val lines = s.text.split('\n')
      lines.find(_.contains('#')) match {
        case Some(line)  => node.location.inputRange.copy(lineTo = node.location.inputRange.lineFrom + lines.indexOf(line), columnTo = line.indexOf('#') -1 )
        case _ => node.location.inputRange
      }
    }else {
      getRefValue.location.inputRange.copy(columnTo = node.location.inputRange.columnTo - fragmentLength)
    }
    Range((inputRange.lineFrom, inputRange.columnFrom + markSize), (inputRange.lineTo, inputRange.columnTo-markSize))
  }

  private def getRefValue = node match {
      case ref: MutRef => ref.origValue
      case _ => node
    }
}

case class CompilerReferenceCollector() {
  private val collector = DefaultReferenceCollector[Reference]()

  def +=(key: String, kind: ReferenceKind, node: YNode): Unit = {
    val (url, fragment) = ReferenceFragmentPartition(key)
    collector.get(url) match {
      case Some(reference: Reference) => collector += (url, reference + (kind, node, fragment))
      case None                       => collector += (url, Reference(url, kind, node, fragment))
    }
  }

  def +=(key: String, kind: ReferenceKind, node: ASTElement): Unit = {
    val (url, fragment) = ReferenceFragmentPartition(key)
    collector.get(url) match {
      case Some(reference: Reference) => collector += (url, reference + (kind, node, fragment))
      case None                       => collector += (url, Reference(url, kind, node, fragment))
    }
  }

  def toReferences: Seq[Reference] = collector.references()
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
